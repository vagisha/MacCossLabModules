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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.panoramapublic.PanoramaPublicController" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        // dependencies.add("Ext4");
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

    ActionURL cancelUrl = PanoramaPublicController.getViewExperimentDetailsURL(form.getId(), form.getContainer());

%>
<labkey:errors/>


<div style="margin-top:15px;">
    <!-- <form id="catalogEntryForm" method="post" enctype="multipart/form-data">-->
    <form id="catalogEntryForm" method="post" enctype="multipart/form-data">
        <labkey:csrf />
        <%=generateReturnUrlFormField(form.getReturnActionURL())%>
        <table class="lk-fields-table">
            <tr>
                <td class="labkey-form-label">Description:</td>
                <td>
                    <textarea id="descFieldInput" rows="8" cols="60" name="datasetDescription" ><%=h(form.getDatasetDescription())%></textarea>
                    <br/>
                    <div id="remainingChars" style="margin-bottom:15px;color:darkgray"><span id="rchars">500</span> characters remaining</div>
                </td>
            </tr>
            <tr>
                <td class="labkey-form-label">Image:</td>
                <td>
                    <input id="imageFileInput" type="file" size="50" style="border: none; background-color: transparent;"
                           accept="image/png,image/jpeg,image/jpg,image.bmp" />
                    <input id="modifiedImage" name="imageFile" type="hidden"/>
                    <input id="imageFileName" name="imageFileName" type="hidden"/>
                    <div id="cropperContainer" style="display:none;margin:10px;padding:15px;" class="cropperBox">
                        <div id="cropperDiv">
                            <div style="margin-bottom:5px">
                            Drag and resize the crop-box over the image, and click the "Crop" button to fit the slideshow dimensions.
                            <span style="margin-left:5px;">
                                <a class="labkey-button" id="btnCrop">Crop</a>
                                <!--<a class="labkey-button" id="btnRestore">Restore</a>-->
                            </span>
                            </div>
                            <canvas id="canvas">
                                Your browser does not support the HTML5 canvas element.
                            </canvas>
                        </div>
                    </div>
                    <div id="preview" style="margin-top:10px; padding:10px;"></div>
                </td>
            </tr>
        </table>
        <br>
        <%=button("Submit").onClick("submitEntry(); return false;")%>
        &nbsp
        <%=button("Cancel").href(cancelUrl)%>
    </form>
</div>



<!-- <link rel="stylesheet" href="https://unpkg.com/bootstrap@4/dist/css/bootstrap.min.css" crossorigin="anonymous">
<script src="https://unpkg.com/bootstrap@4/dist/js/bootstrap.bundle.min.js" crossorigin="anonymous"></script> -->
<!-- <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/cropperjs/1.5.13/cropper.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/cropperjs/1.5.13/cropper.min.js"></script> -->
<!--<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/cropper/2.3.3/cropper.css"/>
<script src="https://cdnjs.cloudflare.com/ajax/libs/cropper/2.3.3/cropper.js"/>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-cropper/1.0.1/jquery-cropper.min.js"></script> -->
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
    .cropperBox {
        box-shadow: 0px 0px 5px darkgray;
        border-radius:5px;
    }

</style>
<script type="text/javascript">

    $(document).ready(function() {

        let cropper;
        let croppedImageDataUrl;

        const canvas  = $("#canvas");
        const context = canvas.get(0).getContext("2d");
        const preview = $('#preview');
        const maxFileSize = 5 * 1024 * 1024;
        const preferredWidth = 600;
        const preferredHeight = 400;

        $('#imageFileInput').on( 'change', function(){

            if (this.files && this.files[0])
            {
                console.log("File selected");
                const file = this.files[0];
                if (!file.type.match(/^image\//))
                {
                    // https://developer.mozilla.org/en-US/docs/Web/Media/Formats/Image_types
                    alert("Invalid file type. Please select an image file (e.g. png, jpeg etc.)");
                    $("#imageFileInput").val($("#imageFileName").val());
                    return;
                }
                if (file.size > maxFileSize)
                {
                    alert("File size cannot be more than 5MB.");
                    $("#imageFileInput").val('');
                    return;
                }

                const fileName = file.name;

                if (cropper)
                {
                    cropper.destroy();
                    context.clearRect(0, 0, canvas.width, canvas.height);
                    $("#imageFileName").val("");
                    preview.empty();
                }
                const reader = new FileReader();
                reader.onload = function(evt) {

                    const img = new Image();
                    img.onload = function() {

                        let w = img.width;
                        let h = img.height;
                        if (w < preferredWidth || h < preferredHeight)
                        {
                            alert("Image must be at least 600 pixels in width and 400 pixels in height. Dimensions of the selected image are: "
                                    + w + " x " + h + " pixels.");
                            return;
                        }

                        // If the image is too large, scale it down for display
                        let scaleFactor = Math.min(900/img.width, 600/img.height);
                        scaleFactor = Math.min(1, scaleFactor);

                        // Get the new width and height based on the scale factor
                        let newWidth = w * scaleFactor;
                        let newHeight = h * scaleFactor;
                        $("#cropperContainer").width(newWidth + 30);
                        context.canvas.width  = newWidth;
                        context.canvas.height = newHeight;

                        // get the top left position of the image
                        // in order to center the image within the canvas
                        // let x = (canvas.width/2) - (newWidth/2);
                        // let y = (canvas.height/2) - (newHeight/2);

                        console.log("Drawing on canvas: " +newWidth + " x " +newHeight);
                        context.drawImage(img, 0, 0, newWidth, newHeight);

                        $("#imageFileName").val(fileName);
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
                            // minContainerWidth: w,
                            // minContainerHeight: h,
                            minCanvasWidth: newWidth,
                            minCanvasHeight: newHeight,
                            minCropBoxWidth: preferredWidth,
                            minCropBoxHeight: preferredHeight
                        });
                        $('#btnCrop').click(function() {
                            croppedImageDataUrl = cropper.getCroppedCanvas(
                                    {width:preferredWidth, height:preferredHeight, imageSmoothingQuality: 'high'})
                                    // https://developer.mozilla.org/en-US/docs/Web/API/HTMLCanvasElement/toDataURL
                                    // https://stackoverflow.com/questions/56573339/using-cropper-js-image-quality-get-reduced-after-cropping
                                    .toDataURL("image/png", 1); // string base 64 data url

                            preview.append( $('<img>').attr('src', croppedImageDataUrl) );
                            $("#modifiedImage").val(croppedImageDataUrl);

                            cropper.destroy();
                            $("#cropperContainer").hide();
                        });
                        $('#btnRestore').click(function() {
                            // canvas.show();
                            cropper.reset();
                            preview.empty();
                        });
                    };
                    img.src = evt.target.result;
                };
                reader.readAsDataURL(this.files[0]);
            }
            else
            {
                alert('Please select a file.');
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

    const maxDescriptionLen = 500;
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

    function submitEntry()
    {
        // alert("Submitting form");
        // var description = $("#descFieldInput").val();
        // console.log("description entered: " + description);
        $("#imageFileInput").val(""); // TODO: No need to send the original file
        $("#catalogEntryForm").submit();
        return;
    }
</script>