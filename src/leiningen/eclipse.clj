(ns leiningen.eclipse
  "Create Eclipse project descriptor files."
  (:use [clojure.java.io :only [file]])
  (:require [clojure.data.xml :as xml])
  (:import [java.io File])
  (:import [java.util.regex Pattern]))

;; copied from jar.clj
(defn- unix-path
  [path]
  (when path
    (.replaceAll path "\\\\" "/")))

;; copied from jar.clj
(defn- trim-leading-str
  [s to-trim]
  (when s
    (clojure.string/replace s (re-pattern (str "^" (Pattern/quote to-trim))) "")))

(defn- directory?
  [arg]
  (when arg
    (.isDirectory (File. arg))))

(defn- list-libraries
  [project]
  (when (:library-path project)
    (map #(when % (.getPath %)) (.listFiles (File. (:library-path project))))))

(defn- create-classpath
  "Print .classpath to *out*."
  [project out-file]
  (let [root (str (unix-path (:root project)) \/)
        noroot  #(trim-leading-str (unix-path %) root)
        [resources-path compile-path source-path test-path]
        (map noroot (map project [:resources-path
                                  :compile-path
                                  :source-path
                                  :test-path]))]
    (xml/emit
     (xml/element :classpath {}
              (if (directory? source-path)
                (xml/element :classpathentry {:kind "src"
                                          :path source-path}))
              (if (directory? resources-path)
                (xml/element :classpathentry {:kind "src"
                                          :path resources-path}))
              (if (directory? test-path)
                (xml/element :classpathentry {:kind "src"
                                          :path test-path}))
              (xml/element :classpathentry {:kind "con"
                                        :path "org.eclipse.jdt.launching.JRE_CONTAINER"})
              (xml/sexp-as-element
               (for [library (list-libraries project)]
                (xml/element :classpathentry {:kind "lib"
                                          :path (noroot library)})))
              (xml/element :classpathentry {:kind "output"
                                        :path compile-path}))
     out-file)))

(defn- create-project
  "Print .project to out-file."
  [project out-file]
  (xml/emit
   (xml/element :projectDescription {}
     (xml/element :name {} (:name project))
     (xml/element :comment {} (:description project))
     (xml/element :projects)
     (xml/element :buildSpec {}
              (xml/element :buildCommand {}
                       (xml/element :name {} "ccw.builder")
                       (xml/element :arguments))
              (xml/element :buildCommand {}
                       (xml/element :name {} "org.eclipse.jdt.core.javabuilder")
                       (xml/element :arguments)))
     (xml/element :natures {}
           (xml/element :nature {} "ccw.nature")
           (xml/element :nature {} "org.eclipse.jdt.core.javanature")))
   out-file))

(defn eclipse
  "Create Eclipse project descriptor files."
  [project]
  (with-open
      [out-file (java.io.FileWriter. (file (:root project) ".classpath"))]
    (create-classpath project out-file))
  (println "Created .classpath")
  (with-open
      [out-file  (java.io.FileWriter. (file (:root project) ".project"))]
    (create-project project out-file))
  (println "Created .project"))
