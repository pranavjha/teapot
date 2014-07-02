/**
 * @fileoverview Defines the class for the home screen
 */
// strict mode
"use strict";
Class.load("teapot.base.UiElement");
Class.load("teapot.util.Logger");
Class.use("teapot.screen.Home");
/**
 * Defines the class for the home screen
 */
teapot.screen.Home = teapot.base.UiElement.extend(
/*
 * public declarations
 */
{
    /**
     * Initializes Home screen
     * @constructor
     * @param {baseElement} $element the jquery base element where the element is to be created
     * @extends {teapot.base.UiElement}
     */
    init : function(baseElement) {
        /**
         * scene object
         * @private
         */
        this._scene;
        /**
         * camera object
         * @private
         */
        this._camera;
        /**
         * renderer object
         * @private
         */
        this._renderer;

        /**
         * the base object to be rendered
         * @private
         */
        this._object;
        /**
         * represents the current animation frame
         */
        this._currentAnimationFrame;
        /**
         * represents the rotation speed for the cube
         */
        this._rotationSpeed = {
            x : 0,
            y : 0.02
        };
        // create the element and initialize the base
        this._super(baseElement);
        // find the viewport
        var viewport = baseElement.find('.viewport');
        var dimensions = {
            'width' : viewport.width(),
            'height' : viewport.height()
        };
        this.constructor.LOG.info('viewport dimensions: ', JSON.stringify(dimensions));
        // create the scene
        this._scene = new THREE.Scene();
        this._camera = new THREE.PerspectiveCamera(75, dimensions.width / dimensions.height, 0.1, 1000);
        this._renderer = new THREE.WebGLRenderer();
        // create the object
        var geometry = new THREE.CubeGeometry(200, 200, 200);
        for ( var i = 0; i < geometry.faces.length; i++) {
            geometry.faces[i].color.setHex(Math.random() * 0xffffff);
        }
        var material = new THREE.MeshBasicMaterial({
            vertexColors : THREE.FaceColors
        });
        this._object = new THREE.Mesh(geometry, material);
        this._object.position.y = 150;

        this._renderer.setSize(dimensions.width, dimensions.height);
        viewport.append($(this._renderer.domElement));
        // add objects to the scene and set the camera
        this._scene.add(this._object);
        this._camera.position.y = 150;
        this._camera.position.z = 500;
        this.renderScene();
        var that = this;
        // remove this closure before destroy
        $(document).off('mousemove.screen').on('mousemove.screen', function(event) {
            that._rotationSpeed.y = -(event.clientX - dimensions.width / 2) * 0.0001;
            that._rotationSpeed.x = -(event.clientY - dimensions.height / 2) * 0.0001;
        });
        // remove this closure before destroy
        $(window).off('resize.screen').on('resize.screen', function(event) {
            var dimensions = {
                'width' : viewport.width(),
                'height' : viewport.height()
            };
            that._camera.aspect = dimensions.width / dimensions.height;
            that._camera.updateProjectionMatrix();

            that._renderer.setSize(dimensions.width, dimensions.height);
        });
    },

    /**
     * disposes off the scene for the dom to be garbage collected
     * @override
     */
    destroy : function() {
        // cancel the animation frame
        cancelAnimationFrame(this._currentAnimationFrame);
        // clear the event handlers
        $(document).off('mousemove.screen');
        $(window).off('resize.screen');
        // call the parent destroy
        this._super();
    },

    /**
     * Renders the scene
     */
    renderScene : function() {
        this._object.rotation.y += this._rotationSpeed.y;
        this._object.rotation.x += this._rotationSpeed.x;
        this._renderer.render(this._scene, this._camera);
        var that = this;
        this._currentAnimationFrame = requestAnimationFrame(function() {
            that.renderScene();
        });
    }
},
/*
 * static declarations
 */
{
    /**
     * @type {string} the style namespace
     * @private
     */
    styleNamespace : "styles.teapot.screen.Home",

    /**
     * @type {string} the template namespace
     * @private
     */
    templateNamespace : "templates.teapot.screen.Home",

    /**
     * the logger utility object
     */
    LOG : teapot.util.Logger.getLogger('teapot.screen.Home')
});