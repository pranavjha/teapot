/**
 * @fileoverview Defines the Command class for representing a basic control for doing actions
 */
//strict mode 
"use strict";
Class.load("teapot.base.UiElement");
Class.load("teapot.util.Logger");
Class.use("teapot.ui.Command");

/**
 * A command control. Basic control for doing actions.
 */
teapot.ui.Command = teapot.base.UiElement.extend(
/*
 * public declarations
 */
{
    /**
     * Instantiates Command Class
     * @constructor
     * @param {baseElement} $element the jquery base element where the element is to be created
     * @param {string} name the command's caption.
     * @param {{top: number, left:number}} spriteposition top and left background position for the sprite
     * @extends {teapot.ui.Command}
     */
    init : function(baseElement, name, spriteposition) {
        this._super(baseElement);
        /**
         * @type {string}
         */
        this.name = name;
        /**
         * @type {{top: number, left:number}}
         */
        this.spriteposition = spriteposition;
    }
},

/*
 * static declarations
 */
{

    /**
     * @type {string} the template namespace
     * @private
     */
    templateNamespace : "template.teapot.ui.Command",

    /**
     * the logger utility object
     */
    LOG : teapot.util.Logger.getLogger('teapot.ui.Command')
});
