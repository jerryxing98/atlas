/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define(['require',
    'backbone',
    'hbs!tmpl/detail_page/DetailPageLayoutView_tmpl',
    'hbs!tmpl/common/buttons_tmpl',
    'utils/Utils',
    'utils/CommonViewFunction',
    'utils/Globals',
    'utils/Enums',
    'utils/Messages',
    'utils/UrlLinks',
    'jquery-ui'
], function(require, Backbone, DetailPageLayoutViewTmpl, ButtonsTmpl, Utils, CommonViewFunction, Globals, Enums, Messages, UrlLinks) {
    'use strict';

    var DetailPageLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends DetailPageLayoutView */
        {
            _viewName: 'DetailPageLayoutView',

            template: DetailPageLayoutViewTmpl,

            /** Layout sub regions */
            regions: {
                REntityDetailTableLayoutView: "#r_entityDetailTableLayoutView",
                RSchemaTableLayoutView: "#r_schemaTableLayoutView",
                RTagTableLayoutView: "#r_tagTableLayoutView",
                RLineageLayoutView: "#r_lineageLayoutView",
                RAuditTableLayoutView: "#r_auditTableLayoutView",
                RProfileLayoutView: "#r_profileLayoutView"

            },
            /** ui selector cache */
            ui: {
                tagClick: '[data-id="tagClick"]',
                propagatedTagDiv: '[data-id="propagatedTagDiv"]',
                title: '[data-id="title"]',
                editButton: '[data-id="editButton"]',
                editButtonContainer: '[data-id="editButtonContainer"]',
                description: '[data-id="description"]',
                editBox: '[data-id="editBox"]',
                deleteTag: '[data-id="deleteTag"]',
                backButton: "[data-id='backButton']",
                addTag: '[data-id="addTag"]',
                tagList: '[data-id="tagList"]',
                propagatedTagList: '[data-id="propagatedTagList"]',
                fullscreenPanel: "#fullscreen_panel"
            },
            templateHelpers: function() {
                return {
                    entityUpdate: Globals.entityUpdate
                };
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.editButton] = 'onClickEditEntity';
                events["click " + this.ui.tagClick] = function(e) {
                    if (e.target.nodeName.toLocaleLowerCase() != "i") {
                        Utils.setUrl({
                            url: '#!/tag/tagAttribute/' + e.currentTarget.textContent,
                            mergeBrowserUrl: false,
                            trigger: true
                        });
                    }
                };
                events["click " + this.ui.deleteTag] = 'onClickTagCross';
                events["click " + this.ui.addTag] = 'onClickAddTagBtn';
                events['click ' + this.ui.backButton] = function() {
                    Backbone.history.history.back();
                };
                return events;
            },
            /**
             * intialize a new DetailPageLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'value', 'collection', 'id', 'entityDefCollection', 'typeHeaders', 'enumDefCollection', 'classificationDefCollection'));
                this.bindEvents();
            },
            bindEvents: function() {
                var that = this;
                this.listenTo(this.collection, 'reset', function() {
                    this.entityObject = this.collection.first().toJSON();
                    var collectionJSON = this.entityObject.entity;
                    // MergerRefEntity.
                    Utils.findAndMergeRefEntity(collectionJSON.attributes, this.entityObject.referredEntities);

                    if (collectionJSON && collectionJSON.guid) {
                        var tagGuid = collectionJSON.guid;
                        this.readOnly = Enums.entityStateReadOnly[collectionJSON.status];
                    } else {
                        var tagGuid = this.id;
                    }
                    if (this.readOnly) {
                        this.$el.addClass('readOnly');
                    } else {
                        this.$el.removeClass('readOnly');
                    }
                    if (collectionJSON) {
                        this.name = Utils.getName(collectionJSON);
                        if (collectionJSON.attributes) {
                            if (this.name && collectionJSON.typeName) {
                                this.name = this.name + ' (' + _.escape(collectionJSON.typeName) + ')';
                            }
                            if (!this.name && collectionJSON.typeName) {
                                this.name = _.escape(collectionJSON.typeName);
                            }
                            this.description = collectionJSON.attributes.description;
                            if (this.name) {
                                this.ui.title.show();
                                var titleName = '<span>' + this.name + '</span>';
                                if (this.readOnly) {
                                    titleName += '<button title="Deleted" class="btn btn-action btn-md deleteBtn"><i class="fa fa-trash"></i> Deleted</button>';
                                }
                                this.ui.title.html(titleName);
                            } else {
                                this.ui.title.hide();
                            }
                            if (this.description) {
                                this.ui.description.show();
                                this.ui.description.html('<span>' + _.escape(this.description) + '</span>');
                            } else {
                                this.ui.description.hide();
                            }
                        }
                        if (collectionJSON.classifications) {
                            this.generateTag(collectionJSON.classifications);
                        } else {
                            this.generateTag([]);
                        }
                        if (Globals.entityTypeConfList && _.isEmptyArray(Globals.entityTypeConfList)) {
                            this.ui.editButtonContainer.html(ButtonsTmpl({ btn_edit: true }));
                        } else {
                            if (_.contains(Globals.entityTypeConfList, collectionJSON.typeName)) {
                                this.ui.editButtonContainer.html(ButtonsTmpl({ btn_edit: true }));
                            }
                        }
                        if (collectionJSON.attributes && collectionJSON.attributes.columns) {
                            var valueSorted = _.sortBy(collectionJSON.attributes.columns, function(val) {
                                return val.attributes.position
                            });
                            collectionJSON.attributes.columns = valueSorted;
                        }
                    }
                    this.hideLoader();
                    var obj = {
                        entity: collectionJSON,
                        guid: this.id,
                        entityName: this.name,
                        typeHeaders: this.typeHeaders,
                        entityDefCollection: this.entityDefCollection,
                        fetchCollection: this.fetchCollection.bind(that),
                        enumDefCollection: this.enumDefCollection,
                        classificationDefCollection: this.classificationDefCollection,
                        attributeDefs: (function() {
                            return that.getEntityDef(collectionJSON);
                        })()
                    }
                    this.renderEntityDetailTableLayoutView(obj);
                    this.renderAuditTableLayoutView(obj);
                    this.renderTagTableLayoutView(obj);
                    if (collectionJSON && (!_.isUndefined(collectionJSON.attributes['profileData']) || collectionJSON.typeName === "hive_db" || collectionJSON.typeName === "hbase_namespace")) {
                        if (collectionJSON.typeName === "hive_db" || collectionJSON.typeName === "hbase_namespace") {
                            this.$('.profileTab a').text("Tables")
                        }
                        this.$('.profileTab').show();
                        this.renderProfileLayoutView(_.extend({}, obj, {
                            entityDetail: collectionJSON.attributes,
                            profileData: collectionJSON.attributes.profileData,
                            typeName: collectionJSON.typeName,
                            value: that.value
                        }));
                    }

                    // To render Schema check attribute "schemaElementsAttribute"
                    var schemaOptions = this.entityDefCollection.fullCollection.find({ name: collectionJSON.typeName }).get('options');
                    if (schemaOptions && schemaOptions.hasOwnProperty('schemaElementsAttribute') && schemaOptions.schemaElementsAttribute !== "") {
                        this.$('.schemaTable').show();
                        this.renderSchemaLayoutView(_.extend({}, obj, {
                            attribute: collectionJSON.attributes[schemaOptions.schemaElementsAttribute]
                        }));
                    }
                }, this);
                this.listenTo(this.collection, 'error', function(model, response) {
                    this.$('.fontLoader').hide();
                    if (response.responseJSON) {
                        Utils.notifyError({
                            content: response.responseJSON.errorMessage || response.responseJSON.error
                        });
                    }
                }, this);
            },
            onRender: function() {
                var that = this;
                Utils.showTitleLoader(this.$('.page-title .fontLoader'), this.$('.entityDetail'));
                this.$('.fontLoader').show(); // to show tab loader
                this.renderLineageLayoutView({
                    guid: this.id,
                    entityDefCollection: this.entityDefCollection,
                    actionCallBack: function() {
                        that.$('#expand_collapse_panel').click();
                    }
                });
                this.$(".resize-graph").resizable({
                    handles: ' s',
                    minHeight: 375,
                    stop: function(event, ui) {
                        that.$('.resize-graph').height(($(this).height()));
                    },
                });
                this.ui.fullscreenPanel.on('fullscreen_done', function(e, panel) {
                    var svgEl = panel.find('.panel-body svg'),
                        scaleEl = svgEl.find('>g'),
                        zoom = that.RLineageLayoutView.currentView.zoom,
                        svg = that.RLineageLayoutView.currentView.svg,
                        viewThis = that.RLineageLayoutView.currentView,
                        setGraphZoomPositionCal = that.RLineageLayoutView.currentView.setGraphZoomPositionCal,
                        zoomed = that.RLineageLayoutView.currentView.zoomed;;

                    if (zoom) {
                        setGraphZoomPositionCal.call(viewThis);
                        zoomed.call(viewThis);
                        if ($(e.currentTarget).find('i').hasClass('fa fa-compress')) {
                            svg.call(zoom)
                                .on("dblclick.zoom", null);

                        } else {
                            svg.call(zoom)
                                .on("wheel.zoom", null)
                                .on("dblclick.zoom", null);
                        }
                    }
                })
            },
            onShow: function() {
                var params = Utils.getUrlState.getQueryParams();
                if (params && params.profile) {
                    this.$('.nav.nav-tabs').find('.profileTab').addClass('active').siblings().removeClass('active');
                    this.$('.tab-content').find('#tab-profile').addClass('active').siblings().removeClass('active');
                    $("html, body").animate({ scrollTop: (this.$('.tab-content').offset().top + 1200) }, 1000);
                }
            },
            fetchCollection: function() {
                this.collection.fetch({ reset: true });
            },
            getEntityDef: function(entityObj) {
                var data = this.entityDefCollection.fullCollection.findWhere({ name: entityObj.typeName }).toJSON();
                var attributeDefs = Utils.getNestedSuperTypeObj({
                    data: data,
                    attrMerge: true,
                    collection: this.entityDefCollection
                });
                return attributeDefs;
            },
            onClickTagCross: function(e) {
                var tagName = $(e.currentTarget).parent().text(),
                    that = this;
                var modal = CommonViewFunction.deleteTagModel({
                    msg: "<div class='ellipsis'>Remove: " + "<b>" + _.escape(tagName) + "</b> assignment from" + " " + "<b>" + this.name + "?</b></div>",
                    titleMessage: Messages.removeTag,
                    buttonText: "Remove"
                });
                if (modal) {
                    modal.on('ok', function() {
                        that.deleteTagData({
                            'tagName': tagName,
                            'guid': that.id,
                            'el': $(e.currentTarget)
                        });
                    });
                    modal.on('closeModal', function() {
                        modal.trigger('cancel');
                    });
                }
            },
            deleteTagData: function(options) {
                var that = this,
                    tagName = options.el.text();
                Utils.showTitleLoader(this.$('.page-title .fontLoader'), this.$('.entityDetail'));
                CommonViewFunction.deleteTag(_.extend({}, options, {
                    callback: function() {
                        that.fetchCollection();
                    }
                }));
            },
            generateTag: function(tagObject) {
                var that = this,
                    tagData = "",
                    propagatedTagListData = "",
                    tag = {
                        'self': [],
                        'propagated': []
                    };
                _.each(tagObject, function(val) {
                    val.entityGuid === that.id ? tag['self'].push(val) : tag['propagated'].push(val);
                });
                _.each(tag.self, function(val) {
                    tagData += '<span class="btn btn-action btn-sm btn-icon btn-blue" title=' + val.typeName + ' data-id="tagClick"><span>' + val.typeName + '</span><i class="fa fa-close" data-id="deleteTag" data-type="tag" title="Delete Tag"></i></span>';
                });
                _.each(tag.propagated, function(val) {
                    propagatedTagListData += '<span class="btn btn-action btn-sm btn-icon btn-blue" title=' + val.typeName + ' data-id="tagClick"><span>' + val.typeName + '</span></span>';
                });
                propagatedTagListData !== "" ? this.ui.propagatedTagDiv.show() : this.ui.propagatedTagDiv.hide();
                this.ui.tagList.find("span.btn").remove();
                this.ui.propagatedTagList.find("span.btn").remove();
                this.ui.tagList.prepend(tagData);
                this.ui.propagatedTagList.html(propagatedTagListData);

            },
            hideLoader: function() {
                Utils.hideTitleLoader(this.$('.page-title .fontLoader'), this.$('.entityDetail'));
            },
            showLoader: function() {
                Utils.showTitleLoader(this.$('.page-title .fontLoader'), this.$('.entityDetail'));
            },
            onClickAddTagBtn: function(e) {
                var that = this;
                require(['views/tag/AddTagModalView'], function(AddTagModalView) {
                    var tagList = [];
                    _.map(that.entityObject.entity.classifications, function(obj) {
                            if (obj.entityGuid === that.id) {
                                tagList.push(obj.typeName);
                            }
                        });
                        var view = new AddTagModalView({
                            guid: that.id,
                            tagList: tagList,
                            callback: function() {
                                that.fetchCollection();
                            },
                            showLoader: that.showLoader.bind(that),
                            hideLoader: that.hideLoader.bind(that),
                            collection: that.classificationDefCollection,
                            enumDefCollection: that.enumDefCollection
                        });
                    view.modal.on('ok', function() {
                        Utils.showTitleLoader(that.$('.page-title .fontLoader'), that.$('.entityDetail'));
                    });
                });
            },
            renderEntityDetailTableLayoutView: function(obj) {
                var that = this;
                require(['views/entity/EntityDetailTableLayoutView'], function(EntityDetailTableLayoutView) {
                    that.REntityDetailTableLayoutView.show(new EntityDetailTableLayoutView(obj));
                });
            },
            renderTagTableLayoutView: function(obj) {
                var that = this;
                require(['views/tag/TagDetailTableLayoutView'], function(TagDetailTableLayoutView) {
                    that.RTagTableLayoutView.show(new TagDetailTableLayoutView(obj));
                });
            },
            renderLineageLayoutView: function(obj) {
                var that = this;
                require(['views/graph/LineageLayoutView'], function(LineageLayoutView) {
                    that.RLineageLayoutView.show(new LineageLayoutView(obj));
                });
            },
            renderSchemaLayoutView: function(obj) {
                var that = this;
                require(['views/schema/SchemaLayoutView'], function(SchemaLayoutView) {
                    that.RSchemaTableLayoutView.show(new SchemaLayoutView(obj));
                });
            },
            renderAuditTableLayoutView: function(obj) {
                var that = this;
                require(['views/audit/AuditTableLayoutView'], function(AuditTableLayoutView) {
                    that.RAuditTableLayoutView.show(new AuditTableLayoutView(obj));
                });
            },
            renderProfileLayoutView: function(obj) {
                var that = this;
                require(['views/profile/ProfileLayoutView'], function(ProfileLayoutView) {
                    that.RProfileLayoutView.show(new ProfileLayoutView(obj));
                });
            },
            onClickEditEntity: function(e) {
                var that = this;
                $(e.currentTarget).blur();
                require([
                    'views/entity/CreateEntityLayoutView'
                ], function(CreateEntityLayoutView) {
                    var view = new CreateEntityLayoutView({
                        guid: that.id,
                        entityDefCollection: that.entityDefCollection,
                        typeHeaders: that.typeHeaders,
                        callback: function() {
                            that.fetchCollection();
                        }
                    });

                });
            }
        });
    return DetailPageLayoutView;
});