/**
 * @fileoverview sets up the screen for load
 */
// strict mode
"use strict";
(function() {
    /**
     * private variable used to keep track of resources loaded and resources scheduled for load
     * @private
     */
    var resourceStatus = {
        added : [],
        loaded : []
    };
    /**
     * loads the javascript and css files in sequence
     * @private
     */
    var loadResources = function(resourceLocations) {
        var head = document.getElementsByTagName('head').item(0);
        for ( var index = 0; index < resourceLocations.length; index++) {
            // check if all depends on have loaded
            var loadable = true;
            // if the script has already loading or loaded, move on
            if (resourceStatus.added.indexOf(resourceLocations[index]['id']) != -1) {
                loadable = false;
            }
            // if all the script dependencies have not loaded, move on
            for ( var depIndex = 0; depIndex < resourceLocations[index]['depends-on'].length; depIndex++) {
                if (resourceStatus.loaded.indexOf(resourceLocations[index]['depends-on'][depIndex]) == -1) {
                    loadable = false;
                    break;
                }
            }
            // if the script is loadable, load it
            if (loadable) {
                // mark it as already loading
                resourceStatus.added.push(resourceLocations[index]['id']);
                var resource;
                switch (resourceLocations[index]['type']) {
                case 'text/javascript':
                    resource = document.createElement('script');
                    resource.setAttribute('type', resourceLocations[index]['type']);
                    resource.setAttribute('id', resourceLocations[index]['id']);
                    resource.setAttribute('src', resourceLocations[index]['src']);
                    break;
                case 'text/css':
                    resource = document.createElement('link');
                    resource.setAttribute('rel', 'stylesheet');
                    resource.setAttribute('type', resourceLocations[index]['type']);
                    resource.setAttribute('id', resourceLocations[index]['id']);
                    resource.setAttribute('href', resourceLocations[index]['src']);
                    break;
                default:
                    throw ("unrecognized resource: " + resourceLocations[index]['id']);
                }
                resource.onload = function() {
                    var _load = ((resourceStatus.loaded.length / resourceLocations.length) * 100).toFixed(2);
                    document.getElementById("load-indicator").style.width = _load + '%';
                    document.getElementById("load-message").innerHTML = this.id + ' loaded... ' + _load + ' %';
                    resourceStatus.loaded.push(this.id);
                    loadResources(resourceLocations);
                };
                head.appendChild(resource);
            }
        }
    };
    loadResources([
    // load the common scripts
    {
        'id' : 'script-common-all',
        'src' : 'script/statics-all.js',
        'type' : 'text/javascript',
        'depends-on' : [ 'templates-common-all' ]
    },
    // load the dwr public api scripts
    {
        'id' : 'dwr-public-all',
        'src' : 'script/dwr-public.js',
        'type' : 'text/javascript',
        'depends-on' : []
    },
    // load the common styles
    {
        'id' : 'style-common-all',
        'src' : 'styles/statics-all.css',
        'type' : 'text/css',
        'depends-on' : []
    },
    // load the common templates
    {
        'id' : 'templates-common-all',
        'src' : 'templates/statics-all.js',
        'type' : 'text/javascript',
        'depends-on' : []
    } ]);
})();