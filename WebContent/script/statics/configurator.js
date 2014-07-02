/**
 * @fileoverview sets up the screen for load
 */
// strict mode
"use strict";
(function() {
    /**
     * Variable to save the currently loaded screen name
     * @private
     */
    var loadedScreenName = null;
    // load the logger and mask
    Class.load("teapot.util.Logger");
    // configure the logger
    teapot.util.Logger.configure([ {
        namespace : "",
        type : teapot.util.Logger.Type.CONSOLE,
        level : teapot.util.Logger.Level.DEBUG,
        output : "[%d{HH:mm:ss:SSS}][%1.1p][%-20.-20c]: %m",
        additivity : true
    } /*
         * , { namespace : "teapot", type : teapot.util.Logger.Type.POPUP, level : teapot.util.Logger.Level.INFO, output :
         * "[{level}][{namespace}][{datetime}]{message}", additivity : false }
         */]);
    Class.load("teapot.util.Mask");
    // override the $.error method to block UI
    $.error = function(message) {
        new teapot.util.Mask($('body'), '<h3>ERROR</h3>' + message,
                "background:rgb(128, 27, 27);padding: 30px;border: none;font-family: Open Sans Light;font-weight: normal;font-size: 20px;text-align: center;");
    };
    var LOG = teapot.util.Logger.getLogger('statics.configurator');
    /**
     * Screen load function.
     */
    $(function() {
        LOG.info('initializing screen..');
        // when no screen is selected, load the home screen
        if (!window.location.hash || !window.location.hash.replace(/#/, '')) {
            LOG.info('no screen selected. navigating to home..');
            window.location.hash = "Home";
        }

        // load the screen and bind screen load on hash change
        $(window).off('hashchange.loadscreen').on('hashchange.loadscreen', function() {
            var screenName = window.location.hash.replace(/^#/, '');
            var mask = new teapot.util.Mask($('body'), 'navigating to ' + screenName);
            LOG.info('hashchange detected.. changing the view to', screenName);
            // destroy the previous screen
            if (loadedScreenName) {
                screen.destroy();
                Class.unLoad(loadedScreenName);
                loadedScreenName = undefined;
            } else {
                // initialize the header and footer
                Class.load('teapot.screen.Footer');
                $('body').empty();
                new teapot.screen.Footer($('body'));
            }
            Class.load('teapot.screen.' + screenName);
            // if screen exists, construct it
            if (teapot.screen[screenName]) {
                screen = new teapot.screen[screenName]($('body'));
                loadedScreenName = 'teapot.screen.' + screenName;
            }
            LOG.info(screenName, 'loaded successfully');
            mask.destroy();
        }).trigger('hashchange');
    });
})();