(ns redditcomments.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe
                                   ;; middleware
                                   debug
                                   ]]
            [goog.events]
            [cljs.pprint :refer [pprint]]
            )
  (:import [goog.net XhrIo]
           [goog Uri]))

(enable-console-print!)

;; init ------------------------------------------------------------------------

(register-handler
 :init-db
 (fn [_ _]
   {:user-name nil
    :error-message nil
    :mode :init
    :comments []}))

;; util ------------------------------------------------------------------------

(defn debug-db []
  (let [DEBUG (r/atom false)]
    (fn []
      [:div.pure-u-1
       [:hr]
       [:button
        {:on-click (fn [ev] (.preventDefault ev) (swap! DEBUG not))}
        (if @DEBUG "off" "on")]
       (when @DEBUG [:pre (with-out-str (pprint @re-frame.db/app-db))])])))

(defn comment-search [user-name before]
  (.send goog.net.XhrIo
         (str "https://api.reddit.com/user/"
              user-name
              "/comments"
              "?limit=100"
              "&after=t1_" before)
         ;; response handler ---------
         (fn [e]
           (let [xhr (.-target e)]
             (if (and
                  (.isSuccess xhr)
                  (= 200 (.getStatus xhr)))
               (let [data (js->clj (.getResponseJson xhr))]
                 (dispatch [:update-comments data]))
               (dispatch [:error "api error"]))))
         ))

;; handlers --------------------------------------------------------------------



(register-handler
 :update-comments
 (fn [db [_ data]]
   (println data)
   db
   ))


(register-handler
 :error
 (fn [db [_ error-message]]
   (assoc db
          :mode :error
          :error-message error-message)))


(register-handler
 :new-search
 (fn [db [_ user-name]]
   (let [new-db (assoc db
                       :mode :searching
                       :comments []
                       :user-name user-name
                       :error-message nil)]
     (comment-search user-name 0)
     new-db)))

;; subs ------------------------------------------------------------------------


;; components ------------------------------------------------------------------


(defn search-component []
  (let [current-input (r/atom "a")]
    (fn []
      [:form {:on-submit
              (fn [ev]
                (.preventDefault ev)
                (dispatch [:new-search @current-input])
                )}
       [:input {:type "text"
                :on-change #(reset! current-input (.. % -target -value))
                :value @current-input}]
       [:button "search"]])))

(defn application []
  [:div.pure-g
   [search-component]

   [debug-db]])

(defonce init
  (dispatch-sync [:init-db]))

(defn mount-root []

  (r/render [#'application]
                  (js/document.getElementById "app")))

(defn ^:export run [] (mount-root))
(defn on-js-reload [] (mount-root))
