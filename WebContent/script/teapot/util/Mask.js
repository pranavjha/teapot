/**
 * @fileoverview Defines the mask UI Element
 */
// strict mode
"use strict";
Class.load("teapot.base.UiElement");
Class.load("teapot.util.Logger");
Class.use("teapot.util.Mask");
/**
 * @class this class defines the mask element
 * @extends {teapot.base.UiElement}
 */
teapot.util.Mask = teapot.base.UiElement.extend(
/*
 * public declarations
 */
{
    /**
     * Initializes the mask class masking the baseElement with the message
     * @param {$} baseElement the element to be masked
     * @param {string=} optional message argument specifying the mask message
     * @param {string=} optional message style argument specifying the mask message style
     */
    init : function(baseElement, message, messageStyle) {
        // if the element is already masked, unmask it
        var _oldMask = baseElement.children('.teapot-util-mask');
        if (_oldMask.size() > 0) {
            this.constructor.LOG.debug(baseElement, 'already masked. removing the old mask.');
            _oldMask.data('class').destroy();
        }
        this.message = message;
        this.messageStyle = messageStyle;
        // create the element and initialize the base
        this.constructor.LOG.debug('masking', baseElement.selector);
        this._super(baseElement);
        // if there is a message, align the mask message to center
        if (message) {
            var _mask = $('#' + this._id);
            var _maskView = _mask.find('.mask-view');
            _maskView.css({
                'top' : (_mask.height() - _maskView.outerHeight(true)) / 2,
                'left' : (_mask.width() - _maskView.outerWidth(true)) / 2
            });
        }
    },
    /**
     * Destroys the mask and removes it from the screen
     */
    destroy : function() {
        this.constructor.LOG.debug('unmasking');
        this._super();
    }
},
/*
 * static declarations
 */
{
    /**
     * @type {string} the style namespace.
     * @private
     */
    styleNamespace : "styles.teapot.util.Mask",

    /**
     * @type {string} the template namespace
     * @private
     */
    templateNamespace : "templates.teapot.util.Mask",

    /**
     * the logger utility object
     */
    LOG : teapot.util.Logger.getLogger('teapot.util.Mask')
});