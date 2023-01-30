<%
    /*
     * Copyright (c) 2008-2019 LabKey Corporation
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.panoramapublic.catalog.CatalogEntrySettings" %>
<%@ page import="org.labkey.panoramapublic.query.CatalogEntryManager" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("PanoramaPublic/cropperjs/cropper.min.js");
        dependencies.add("PanoramaPublic/cropperjs/cropper.min.css");
        dependencies.add("PanoramaPublic/cropperjs/jquery-cropper.min.js");
        dependencies.add("PanoramaPublic/jQuery/jquery-ui.min.css");
        dependencies.add("PanoramaPublic/jQuery/jquery-ui.min.js");
    }
%>
<%
    JspView<PanoramaPublicController.CatalogEntryBean> view = (JspView<PanoramaPublicController.CatalogEntryBean>) HttpView.currentView();
    var bean = view.getModelBean();
    var form = bean.getForm();

    var attachedFile = bean.getImageFileName();
    var attachmentUrl = bean.getImageUrl();

    CatalogEntrySettings settings = CatalogEntryManager.getCatalogEntrySettings();
    int descriptionCharLimit = settings.getMaxTextChars();
    long maxFileSize = settings.getMaxFileSize();
    String maxFileSizeMb = FileUtils.byteCountToDisplaySize(maxFileSize);
    int minImageWidth = settings.getMinImgWidth();
    int minImageHeight = settings.getMinImgHeight();

    ActionURL cancelUrl = PanoramaPublicController.getViewExperimentDetailsURL(form.getId(), form.getContainer());

%>
<labkey:errors/>

<style>
    /* Ensure the size of the image fit the container perfectly */
    .cropperBox img {
        display: block;
        /* This rule is very important, please don't ignore this */
        max-width: 100%;
    }
    #canvas {
        background-color: #ffffff;
        cursor: default;
        border: 1px solid black;
    }
    .noRemainingChars
    {
        color:red;
    }
    #mask{
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: #000;
        display: none;
        z-index: 10000;
    }
    #cropperContainer
    {
        position: absolute;
        left: 50%;
        top: 50%;
        transform: translate(-50%, -50%);
        margin: 10px auto;
        padding: 15px;
        background-color: #ffffff;
        cursor: pointer;
        z-index: 10001;
        display: none;
        box-shadow: 0px 0px 5px darkgray;
        border-radius:5px;
    }
</style>

<div style="margin-top:15px;">
    <form id="catalogEntryForm" method="post" enctype="multipart/form-data">
        <labkey:csrf />
        <%=generateReturnUrlFormField(form.getReturnActionURL())%>
        <table class="lk-fields-table">
            <tr>
                <td class="labkey-form-label" style="text-align:center;">Description:</td>
                <td>
                    <textarea id="descFieldInput" rows="8" cols="60" name="datasetDescription" ><%=h(form.getDatasetDescription())%></textarea>
                    <br/>
                    <div id="remainingChars" style="margin-bottom:15px;color:darkgray">
                        <span id="rchars"><%=descriptionCharLimit%></span> characters remaining
                    </div>
                </td>
            </tr>
            <tr>
                <td class="labkey-form-label" style="text-align:center;">Image:</br>
                    <span style="font-size:0.8em;">png, jpeg</span>
                    <br>
                    <span style="font-size:0.8em;">Size: < <%=h(maxFileSizeMb)%></span>
                    <br>
                    <span style="font-size:0.8em;">Preferred: <%=minImageWidth%> x <%=minImageHeight%> pixels</span>
                </td>
                <td>
                    <input id="imageFileInput" type="file" size="50" style="border: none; background-color: transparent;" accept="image/png,image/jpeg" />
                    <input id="modifiedImage" name="imageFile" type="hidden"/>
                    <input id="imageFileName" name="imageFileName" type="hidden"/>
                    <div id="preview" style="margin-top:10px; padding:10px;"></div>
                </td>
            </tr>
        </table>
        <br>
        <%=button("Submit").submit(true).disableOnClick(true)%>
        &nbsp
        <%=button("Cancel").href(cancelUrl)%>
    </form>
</div>
<!-- Example modal div: http://jsfiddle.net/r77K8/1/ -->
<div id='mask'></div>
<div id="cropperContainer">
    <div id="cropperDiv">
        <canvas id="canvas">
            Your browser does not support the HTML5 canvas element.
        </canvas>
    </div>
    <div style="margin-top:5px; text-align:center">
        Drag and resize the crop-box over the image, and click the "Crop" button to fit the slideshow dimensions.
        <div style="margin-top:5px;">
            <a class="labkey-button" id="btnCrop">Crop</a>
            <a class="labkey-button" style="margin-left:5px;" id="btnCancel">Cancel</a>
        </div>
    </div>
</div>

<script type="text/javascript">

    let cropper;
    let croppedImageDataUrl;
    let fileName;
    let canvas;
    let context;
    let preview;

    const maxFileSize = <%=maxFileSize%>;
    const maxFileSizeMb = <%=h(maxFileSizeMb)%>
    const preferredWidth = <%=minImageWidth%>;
    const preferredHeight = <%=minImageHeight%>;
    const maxDisplayWidth = Math.max(900, preferredWidth);
    const maxDisplayHeight = Math.max(600, preferredHeight);

    (function($) {

        $(document).ready(function() {

            canvas  = $("#canvas");
            context = canvas.get(0).getContext("2d");
            preview = $('#preview');

            <% if (attachedFile != null) { %>
                // If we are editing an entry, show the attached image file in the preview box.
                preview.append($('<img>').attr('src', <%=q(attachmentUrl)%>));
                preview.append($('<div>').text(<%=q(attachedFile)%>)); // jQuery escapes the provided string (http://api.jquery.com/text/#text2)
            <% } %>

            $('#imageFileInput').on( 'change', function(){

                if (this.files && this.files[0])
                {
                    const file = this.files[0];
                    fileName = file.name;
                    if (!(file.type.match(/^image\/png/) || file.type.match(/^image\/jpeg/)))
                    {
                        // https://developer.mozilla.org/en-US/docs/Web/Media/Formats/Image_types
                        alert("Invalid file type. Please select an image file (e.g. png, jpeg).");
                        $("#imageFileInput").val('');
                        return;
                    }
                    if (file.size > maxFileSize)
                    {
                        alert("File size cannot be more than " + maxFileSizeMb + ".");
                        $("#imageFileInput").val('');
                        return;
                    }
                    displayCropper(this.files[0]);
                }
            });

            $('#btnCrop').click(function() {
                croppedImageDataUrl = cropper.getCroppedCanvas(
                        {width:preferredWidth, height:preferredHeight, imageSmoothingQuality: 'high'})
                        // https://developer.mozilla.org/en-US/docs/Web/API/HTMLCanvasElement/toDataURL
                        // https://stackoverflow.com/questions/56573339/using-cropper-js-image-quality-get-reduced-after-cropping
                        .toDataURL("image/png", 1); // string base 64 data url

                preview.empty(); // Clear the preview div first
                preview.append($('<img>').attr('src', croppedImageDataUrl));
                preview.append($('<div>').text(fileName)); // jQuery escapes the provided string (http://api.jquery.com/text/#text2)
                preview.append($('<a>').addClass("labkey-button").attr('id', "btnEdit")
                        .text("Edit").click(function() {
                            const files = $('#imageFileInput').prop('files');
                            if (files && files[0])
                            {
                                displayCropper(files[0]);
                            }
                            else
                            {
                                alert("No file is selected.");
                            }
                        }));

                $("#imageFileName").val(fileName);
                $("#modifiedImage").val(croppedImageDataUrl);
                clearCropper(cropper);
            });
            $('#btnCancel').click(function() {
                clearCropper(cropper);
                const selectedFile = $("#imageFileInput").prop('files') ? $("#imageFileInput").prop('files')[0] : null;
                if (selectedFile && selectedFile.name !== $("#imageFileName").val())
                {
                    $("#imageFileInput").val('');
                }
            });

            const descInput = $('#descFieldInput')
            if (descInput.val().length > 0)
            {
                limitDescription(descInput);
            }
            descInput.keyup(function()
            {
                limitDescription($(this));
            });
            descInput.keypress(function(e) {
                if ($(this).val().length === maxDescriptionLen)
                {
                    e.preventDefault();
                }
            });
        });

        const maxDescriptionLen = <%=descriptionCharLimit%>>;
        function limitDescription(inputField)
        {
            inputField.val(inputField.val().substring(0, maxDescriptionLen));
            const remaining = maxDescriptionLen - inputField.val().length;
            $('#rchars').text(remaining);
            const remainingChars = $("#remainingChars");
            const hasNoRemainingCls = remainingChars.hasClass("noRemainingChars");
            if (remaining <= 0) {
                if (!hasNoRemainingCls) remainingChars.addClass("noRemainingChars");
            }
            else {
                if (hasNoRemainingCls) remainingChars.removeClass("noRemainingChars");
            }
        }

        function displayCropper(file)
        {
            const reader = new FileReader();
            reader.onload = function(evt) {

                const img = new Image();
                img.onload = function() {

                    let w = img.width;
                    let h = img.height;
                    if (w < preferredWidth || h < preferredHeight)
                    {
                        alert("Image must be at least " + preferredWidth + " pixels in width and "
                                + preferredHeight + " pixels in height. Dimensions of the selected image are: "
                                + w + " x " + h + " pixels.");
                        $("#imageFileInput").val('');
                        return;
                    }

                    clearCropper(cropper);

                    // If the image is too large, scale it down for display
                    let scaleFactor = Math.min(maxDisplayWidth/img.width, maxDisplayHeight/img.height);
                    scaleFactor = Math.min(1, scaleFactor);

                    // Get the new width and height based on the scale factor
                    let newWidth = w * scaleFactor;
                    let newHeight = h * scaleFactor;
                    $("#cropperContainer").width(newWidth + 30);
                    context.canvas.width  = newWidth;
                    context.canvas.height = newHeight;

                    context.drawImage(img, 0, 0, newWidth, newHeight);

                    $("#mask").fadeTo(500, 0.60);
                    $("#cropperContainer").show();

                    cropper = new Cropper(document.getElementById("canvas"), {
                        viewMode: 2,
                        dragMode: 'move',
                        aspectRatio: 3 / 2,
                        cropBoxResizable: true,
                        movable: false,
                        rotatable: false,
                        scalable: false,
                        zoomable: false,
                        zoomOnTouch: false,
                        zoomOnWheel: false,
                        cropBoxMovable: true,
                        cropBoxResizable: true,
                        toggleDragModeOnDblclick: false,
                        autoCrop: true,
                        autoCropArea: 1, // If the image is already the preferred size (600 x 400) the crop-box should fit exactly
                        minCanvasWidth: newWidth,
                        minCanvasHeight: newHeight,
                        minCropBoxWidth: preferredWidth,
                        minCropBoxHeight: preferredHeight,
                        restore: false // set to false to avoid problems when resizing the browser window
                                       // (https://github.com/fengyuanchen/cropper/issues/488)
                    });
                };
                img.src = evt.target.result;
            };
            reader.readAsDataURL(file);
        }
        function clearCropper(cropper)
        {
            if (cropper) cropper.destroy();
            $("#cropperContainer").hide();
            $("#mask").hide();
            if (canvas && context) { context.clearRect(0, 0, canvas.width, canvas.height); }
        }
    })(jQuery);

</script>