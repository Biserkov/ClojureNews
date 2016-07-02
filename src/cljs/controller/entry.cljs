(ns controller.entry
  (:require [ajax.core :as ajax :refer [GET POST PUT]]
            [reagent.core :as r]
            [goog.dom :as dom]
            [util.view]
            [util.controller]
            [view.entry]
            [view.story-entry]
            [controller.upvote]
            [controller.comment-entry]
            [cljc.validation :as validation]))

(declare add-event-listener-to-add-comment-button
         add-event-listener-to-upvote-buttons
         add-event-listener-to-edit-story-button)

(defn home-page
  []
  (GET "/entry"
       {:handler         (fn [response]
                           (r/render-component [(fn []
                                                  (view.entry/component-story-and-ask response))] util.view/main-container))
        :error-handler   util.controller/error-handler
        :format          (ajax/json-request-format)
        :response-format (ajax/json-response-format {:keywords? true})}))

(defn get-story-by-id
  [id]
  (GET (str "/entry/story/" id)
       {:handler         (fn [response]
                           (r/render-component [(fn []
                                                  (view.story-entry/component-story response))] util.view/main-container)

                           (add-event-listener-to-add-comment-button get-story-by-id id)
                           (add-event-listener-to-upvote-buttons response))
        :error-handler   util.controller/error-handler
        :format          (ajax/json-request-format)
        :response-format (ajax/json-response-format {:keywords? true})}))

(defn edit-story-by-id
  [id]
  (GET (str "/entry/story/info/" id)
       {:handler         (fn [response]
                           (r/render-component [(fn []
                                                  (view.story-entry/component-edit response))] util.view/main-container)
                           (add-event-listener-to-edit-story-button id))
        :error-handler   util.controller/error-handler
        :format          (ajax/json-request-format)
        :response-format (ajax/json-response-format {:keywords? true})}))

(defn edit-story
  [id field-ids]
  (let [data (util.view/create-field-val-map field-ids)
        title (:title data)]

    (cond
      (not (validation/submit-title? title))
      (util.view/render-error-message "Please limit title to 80 characters.")

      :else
      (POST (str "/entry/story/edit/" id)
            {:params          data
             :handler         (fn [_]
                                (util.view/change-url (str "/#/story/" id)))
             :error-handler   util.controller/error-handler
             :format          (ajax/json-request-format)
             :response-format (ajax/json-response-format {:keywords? true})}))))

(defn add-event-listener-to-add-comment-button
  [entry id]
  (.addEventListener (dom/getElement "buttonAddCommentId") "click" (fn [_]
                                                                     (controller.comment-entry/add-comment entry id ["textId"]))))
(defn add-event-listener-to-upvote-buttons
  [response]
  (doseq [commentt (-> response :story-comments)]
    (let [comment-id (:_id commentt)
          upvoted-comments (-> response :story-upvoted-comments)]
      (when-not (util.view/in? comment-id upvoted-comments)
        (when-let [node (dom/getElement (str "id-upvote-" comment-id))]
          (.addEventListener node "click" (fn [_]
                                            (controller.upvote/upvote-story-comment comment-id))))))))

(defn add-event-listener-to-edit-story-button
  [id]
  (.addEventListener (dom/getElement "buttonStoryEditId") "click" (fn [_]
                                                                    (edit-story id ["titleId"]))))

