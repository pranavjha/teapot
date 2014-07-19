/**
 * @fileoverview Defines a singleton class provides static utility functions for teapot
 */
// strict mode
"use strict";
Class.use("teapot.util.StaticUtils");
/**
 * This singleton class provides static utility functions for teapot
 * @expose
 */
teapot.util.StaticUtils = Class.extend({},
/*
 * static declarations
 */
{
    /**
     * Generates a unique ID
     * @returns a unique string id
     * @expose
     */
    uniqueId : function() {
        var s4 = function() {
            return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
        };
        return s4() + s4() + s4() + s4() + s4() + s4() + s4() + s4();
    }
});