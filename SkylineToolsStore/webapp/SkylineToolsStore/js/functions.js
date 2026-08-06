/*
 * Copyright (c) 2017-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
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
    // Moving the field into the wrapper detaches it, which drops any focus already on it. The
    // manage owners dialog focuses the field before this runs, so put it back.
    var hadFocus = input.is(":focus");
    input.after(wrap);
    wrap.append(input);
    if (hadFocus)
        input.trigger("focus");
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
        var item = items.eq(activeIndex).addClass("active");
        input.attr("aria-activedescendant", item.children("a").attr("id"));
        scrollIntoMenu(item[0]);
    }

    // The menu scrolls once it passes its capped height, so the highlight has to be brought into
    // the menu's own box. scrollIntoView would move the page instead.
    function scrollIntoMenu(item) {
        var box = menu[0];
        var top = item.offsetTop;
        var bottom = top + item.offsetHeight;
        if (top < box.scrollTop)
            box.scrollTop = top;
        else if (bottom > box.scrollTop + box.clientHeight)
            box.scrollTop = bottom - box.clientHeight;
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
            // The li is presentational, so the option counts as a direct child of the listbox for
            // a screen reader. Bootstrap's dropdown-menu styling needs the li to stay in the markup.
            $('<li role="presentation"></li>').append(
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

    // Typing only. Rendering on focus as well dropped the whole list open the moment a dialog
    // focused the field, and reopened it right after a name was picked, since choose() refocuses.
    // The Down arrow still opens it on demand.
    input.on("input", render);

    input.on("keydown", function(e) {
        var items = menu.children();
        switch (e.keyCode) {
            case 27:                                     // Escape
                // Bootstrap's modal hides on any Escape that reaches it, and does not check
                // preventDefault, so the event has to be stopped before it bubbles that far.
                // Only while the menu is open - with it closed, Escape should still close the
                // dialog around the field.
                if (isOpen()) {
                    e.stopPropagation();
                    e.preventDefault();
                }
                close();
                break;
            case 9:                                      // Tab
                // Tab completes the highlighted address, as the jQuery UI widget did. It suppressed
                // the focus move for this case only, so the field keeps focus and the next address
                // can be typed. With nothing highlighted, Tab just moves on.
                // Shift+Tab is leaving the field backwards, never a completion.
                if (!e.shiftKey && isOpen() && activeIndex >= 0) {
                    e.preventDefault();
                    choose(items.eq(activeIndex).children("a").text());
                }
                close();
                break;
            case 38:                                     // Up
            case 40:                                     // Down
                e.preventDefault();
                if (!isOpen()) {
                    render();
                    // Nothing matched, so there is nothing to move through.
                    if (!isOpen())
                        break;
                }
                // Re-read after render, which empties and rebuilds the list. The snapshot taken at
                // the top of this handler is the menu as it was before the key opened it.
                items = menu.children();
                // From -1 this lands on the first entry going down and the last going up, so the
                // key that opened the menu also highlights something.
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
    // menu opens from typing and has no toggle, so it needs its own outside click handler. One
    // handler serves every field on the page, rather than one per call, which would accumulate.
    autocomplete._open = autocomplete._open || [];
    autocomplete._open.push({wrap: wrap, close: close});
    if (!autocomplete._closeBound) {
        autocomplete._closeBound = true;
        $(document).on("click", function(e) {
            autocomplete._open.forEach(function(entry) {
                if (entry.wrap[0] !== e.target && !$.contains(entry.wrap[0], e.target))
                    entry.close();
            });
        });
    }
}

function getCookie(name) {
    var parts = document.cookie.split(name + "=");
    if (parts.length == 2)
        return parts.pop().split(";").shift();
    return null;
}

