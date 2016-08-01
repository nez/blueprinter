(ns blueprinter.core
  (:require [clojure.set :refer :all]
            [clojure.string :as s]
            [formaterr.core :refer :all]
            [digitalize.core :refer :all]
            [dora.util :refer :all]))

(def bp-defaults
  {:input {}
   :output {}
   :triggers []})

(def t-structure
  [:triggerObject :triggerName :triggerArguments
   :actionObject :actionName :actionArguments :actionOutput] )
(defn trigger [m])

(defn input [])
(defn output [])
(defn triggers [])

(defn blueprint [])

(def bp-inputs
  #{:BlueprintInputMapTiles
    :BlueprintInputGeoJSON
    :BlueprintInputKML
    :BlueprintInputGPX})

(def input-mapper
  {:tiles :BlueprintInputMapTiles
   :json  :BlueprintInputGeoJSON
   :kml   :BlueprintInputKML
   :gpx   :BlueprintInputGPX})

(def bp-outputs
  #{})

(def data-mapping
  {:data
   {;Loop through each item in triggerArg.kml and return a new array of processed values (a map)
    :process "map"
    ;Name of trigger argument
    :itemsObject "kml"
    ;Within kml the data is stored in the document.placemark array
    :itemsProperties "document.placemark"
    ;Return a new object for each document.placemark item with the given propertiea
    :transformation {
                     ;Eg. document.placemark[n].point.coordinates
                     :coordinates "point.coordinates"
                     }}})


(defn csv-blueprint
  ([path]
   (csv-blueprint path ["longitude" "latitude"]))
  ([path coordinates]
   {:input {:type "BlueprintInputCSV"
            :options {:path path}}
    :output
    {:type "BlueprintOutputBars"
     :options
     {:workerURL "js/lib/vizi-worker.js"
      :height 20
      :radius 20
      :materialType "MeshPhongMaterial"
      :materialOptions
      {:transparent true
       :opacity 0.4
       :color 4473328
       :emissive 4473328}}}
    :triggers
    [{:triggerObject "output"
      :triggerName "initialised"
      :triggerArguments []
      :actionObject "input"
      :actionName "requestData"
      :actionArguments []
      :actionOutput {}}
     {:triggerObject "input"
      :triggerName "dataReceived"
      :triggerArguments ["data"]
      :actionObject "output"
      :actionName "outputBars"
      :actionArguments ["data"]
      :actionOutput
      {:data
       {:process "map"
        :itemsObject "data"
        :itemsProperties "data"
        :transformation {:coordinates coordinates}}}}]}))

                                        ;todo: podemos sacar las coordinates a partir del path
                                        ;color puede ser random si no se asigna

(def vizi-location "/Users/nex/github/webvr-cities/")

(defn extension [f ext]
  (str (name f) "." (name ext)))

(defn data-path [name]
  (str "data/" name))

(defn csv-data-path [name]
  (data-path (extension name "csv")))

(defn make-layer [file path]
  (spit (str vizi-location "layers/" (extension file :json)) (json (csv-blueprint (data-path path)))))

(defmulti layer-template category)

(defmethod layer-template :string [s]
  (s/replace "'name': true" #"name" s))

(defmethod layer-template :coll [coll]
  (s/join ",\n		" (map layer-template coll)))

(defn dataviz-string
  ([name]
   (dataviz-string name name))
  ([name layers]
   (str
    "window.dataViz('" name "', {
	layers: {
		" (layer-template layers) "
	},
	info: [
		'<div></div>'
	].join('')});")))

(defn make-dataviz
  ([name]
   (make-dataviz name name))
  ([name layers]
   (spit (str vizi-location "js/dataviz/" (extension name :js))
         (dataviz-string name layers))))

(defn vizi-add-csv
  ([name]
   (vizi-add-csv name (extension name :csv)))
  ([name path]
   (make-layer name path)
   (make-dataviz name)))

(defn option [val name]
  (str "<option value=\"" val "\">" name "</option>"))

(defn options [m]
  (s/join "\n" (map option (map name (keys m)) (vals m))))

(defn vizi-add-denue
  [name] (vizi-add-csv name (str "denue/" name ".csv")))
