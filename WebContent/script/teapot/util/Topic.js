/**
 * @fileoverview Defines the teapot publish subscribe framework
 */
// strict mode
"use strict";
Class.use("teapot.util.Topic");
/**
 * @class The teapot publish subscribe framework
 */
teapot.util.Topic = Class.extend(
/*
 * public declarations
 */
{
    /**
     * constructor for the Topic class. Creates a new Topic.
     * @constructor
     * @private
     */
    init : function() {
        this.topic = $.Callbacks();
    },
    /**
     * Publishes the topic so that all the subscribers are intimidated
     * @param {...*} arguments the data to publish
     * @returns {teapot.util.Topic} for call chaining
     */
    publish : function() {
        this.topic.fire.apply(null, arguments);
        return this;
    },

    /**
     * Used to subscribe to this topic
     * @param {function(...*)} subscriber the subscriber function
     * @returns {teapot.util.Topic} for call chaining
     */
    subscribe : function(subscriber) {
        this.topic.add(subscriber);
        return this;
    },

    /**
     * Used to un-subscribe to this topic
     * @param {function(...*)} subscriber the subscriber function
     * @returns {teapot.util.Topic} for call chaining
     */
    unsubscribe : function(subscriber) {
        this.topic.remove(subscriber);
        return this;
    }
},
/*
 * static declarations
 */
{
    /**
     * the static list of all topics
     * @private
     */
    __topics : {},
    
    /**
     * Returns the Topic corresponding to the topic name. If the topic is not available, it creates one
     * @param {string} topicName the naem of the topic
     * @returns {teapot.util.Topic}
     */
    get : function(topicName) {
        return (this.__topics[topicName] || (this.__topics[topicName] = new teapot.util.Topic()));
    }
});