/*
 * Copyright (c) 2017-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function setButtonsEnabled(enable) {
    var action = enable ? "enable" : "disable";
    $(".ui-dialog-buttonpane button:contains('Ok')").button(action);
    $(".ui-dialog-buttonpane button:contains('Cancel')").button(action);
}

/**
 * Type-ahead for a field holding a comma separated list of addresses.
 *
 * The menu is a Bootstrap dropdown so it takes the theme's styling, but the behaviour is written
 * here. Bootstrap's own data-api only opens and closes a menu, it does not filter or complete.
 *
 * txtbox - jQuery object holding the input
 * tags   - array of candidate addresses. Callers who may not see the list pass "" instead, and the
 *          field is then left as a plain text box.
 */
function autocomplete(txtbox, tags) {
    var input = txtbox.first();
    if (input.length === 0 || !Array.isArray(tags) || tags.length === 0)
        return;

    autocomplete._seq = (autocomplete._seq || 0) + 1;
    var menuId = "autocompleteMenu" + autocomplete._seq;
    var activeIndex = -1;

    // Bootstrap places .dropdown-menu against the nearest positioned .dropdown, so the field is
    // wrapped in one. A span rather than a div, because two of these fields sit inside a paragraph.
    var wrap = $('<span class="dropdown autocompleteWrap"></span>');
    input.after(wrap);
    wrap.append(input);
    var menu = $('<ul class="dropdown-menu autocompleteMenu" role="listbox"></ul>').attr("id", menuId);
    wrap.append(menu);

    input.attr({
        "role": "combobox",
        "aria-autocomplete": "list",
        "aria-expanded": "false",
        "aria-controls": menuId,
        "autocomplete": "off"
    });

    // Only the text after the last comma is being typed. Anything before it is already chosen.
    function typedTerm() {
        return input.val().split(",").pop().trim().toLowerCase();
    }

    function isOpen() {
        return wrap.hasClass("open");
    }

    function close() {
        wrap.removeClass("open");
        input.attr("aria-expanded", "false").removeAttr("aria-activedescendant");
        activeIndex = -1;
    }

    // A screen reader follows aria-activedescendant, so the highlight and that attribute have to
    // move together.
    function highlight() {
        var items = menu.children();
        items.removeClass("active");
        if (activeIndex < 0) {
            input.removeAttr("aria-activedescendant");
            return;
        }
        input.attr("aria-activedescendant",
                items.eq(activeIndex).addClass("active").children("a").attr("id"));
    }

    function render() {
        var term = typedTerm();
        var matches = tags.filter(function(tag) {
            return tag.toLowerCase().indexOf(term) !== -1;
        });

        menu.empty();
        activeIndex = -1;
        if (matches.length === 0) {
            close();
            return;
        }
        matches.forEach(function(value, i) {
            $("<li></li>").append(
                    $('<a href="#" role="option"></a>').attr("id", menuId + "-" + i).text(value)
            ).appendTo(menu);
        });
        wrap.addClass("open");
        input.attr("aria-expanded", "true");
        highlight();
    }

    // Replace the term being typed, keep the ones already chosen, and leave a trailing separator so
    // the next address can be typed straight away.
    function choose(value) {
        var terms = input.val().trim().split(/\s*,\s*/);
        terms.pop();
        if (terms.indexOf(value) === -1)
            terms.push(value);
        terms.push("");
        input.val(terms.join(", "));
        close();
        input.focus();
    }

    menu.on("click", "a", function(e) {
        e.preventDefault();
        choose($(this).text());
    });

    input.on("input focus", render);

    input.on("keydown", function(e) {
        var items = menu.children();
        switch (e.keyCode) {
            case 27:                                     // Escape
            case 9:                                      // Tab, and let the focus move on
                close();
                break;
            case 38:                                     // Up
            case 40:                                     // Down
                e.preventDefault();
                if (!isOpen()) {
                    render();
                    break;
                }
                activeIndex += (e.keyCode === 40 ? 1 : -1);
                if (activeIndex >= items.length)
                    activeIndex = 0;
                else if (activeIndex < 0)
                    activeIndex = items.length - 1;
                highlight();
                break;
            case 13:                                     // Enter
                if (isOpen() && activeIndex >= 0) {
                    // Picking a name here must not also submit the form around the field.
                    e.preventDefault();
                    choose(items.eq(activeIndex).children("a").text());
                }
                break;
        }
    });

    // Bootstrap's clearMenus only closes menus whose toggle carries data-toggle="dropdown". This
    // menu opens from typing and has no toggle, so it needs its own outside click handler.
    $(document).on("click", function(e) {
        if (wrap[0] !== e.target && !$.contains(wrap[0], e.target))
            close();
    });
}

function getCookie(name) {
    var parts = document.cookie.split(name + "=");
    if (parts.length == 2)
        return parts.pop().split(";").shift();
    return null;
}

function fixDlg(dlg) {
    dlg.parent().css("position", "fixed");
}

function initJqueryUiImages(dir) {
    function setBg(elements, img) {
        $(elements).each(function() {$(this).css("background-image", "url(" + dir + "/" + img + ")");});
    }
    setBg(".ui-progressbar > .ui-progressbar-overlay", "animated-overlay.gif");
    setBg(".ui-widget-content", "ui-bg_flat_75_ffffff_40x100.png");
    setBg(".ui-widget-header", "ui-bg_highlight-soft_75_cccccc_1x100.png");
    setBg(".ui-state-default", "ui-bg_glass_75_e6e6e6_1x400.png");
    setBg(".ui-state-hover, .ui-state-focus", "ui-bg_glass_75_dadada_1x400.png");
    setBg(".ui-state-active", "ui-bg_glass_65_ffffff_1x400.png");
    setBg(".ui-state-highlight", "ui-bg_glass_55_fbf9ee_1x400.png");
    setBg(".ui-state-error", "ui-bg_glass_95_fef1ec_1x400.png");
    setBg(".ui-icon", "ui-icons_222222_256x240.png");
    setBg(".ui-state-default > .ui-icon", "ui-icons_888888_256x240.png");
    setBg(".ui-state-hover > .ui-icon, .ui-state-focus > .ui-icon, .ui-state-active > .ui-icon", "ui-icons_454545_256x240.png");
    setBg(".ui-state-highlight > .ui-icon", "ui-icons_2e83ff_256x240.png");
    setBg(".ui-state-error > .ui-icon, .ui-state-error-text > .ui-icon", "ui-icons_cd0a0a_256x240.png");
    setBg(".ui-widget-overlay", "ui-bg_flat_0_aaaaaa_40x100.png");
    setBg(".ui-widget-shadow", "ui-bg_flat_0_aaaaaa_40x100.png");
}
