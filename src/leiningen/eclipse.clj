(ns leiningen.eclipse
  "Create Eclipse project descriptor files."
  (:use [clojure.java.io :only [file]])
  (:use [clojure.data.xml :only [emit element]])
  (:import [java.io File])
  (:import [java.util.regex Pattern]))

;; copied from jar.clj
(defn- unix-path
  [path]
  (.replaceAll path "\\\\" "/"))

;; copied from jar.clj
(defn- trim-leading-str
  [s to-trim]
  (clojure.string/replace s (re-pattern (str "^" (Pattern/quote to-trim))) ""))

(defn- directory?
  [arg]
  (.isDirectory (File. arg)))

(defn- list-libraries
  [project]
  (map #(.getPath %) (.listFiles (File. (:library-path project)))))

(defn- create-classpath
  "Print .classpath to *out*."
  [project]
  (let [root (str (unix-path (:root project)) \/)
        noroot  #(trim-leading-str (unix-path %) root)
        [resources-path compile-path source-path test-path]
        (map noroot (map project [:resources-path
                                  :compile-path
                                  :source-path
                                  :test-path]))]
    (emit
     (element :classpath {}
              (if (directory? source-path)
                (element :classpathentry {:kind "src"
                                          :path source-path}))
              (if (directory? resources-path)
                (element :classpathentry {:kind "src"
                                          :path resources-path}))
              (if (directory? test-path)
                (element :classpathentry {:kind "src"
                                          :path test-path}))
              (element :classpathentry {:kind "con"
                                        :path "org.eclipse.jdt.launching.JRE_CONTAINER"})
              (for [library (list-libraries project)]
                (element :classpathentry {:kind "lib"
                                          :path (noroot library)}))
              (element :classpathentry {:kind "output"
                                        :path compile-path})))))

(defn- create-project
  "Print .project to out-file."
  [project out-file]
  (emit
   (element :projectDescription {}
     (element :name {} (:name project))
     (element :comment {} (:description project))
     (element :projects)
     (element :buildSpec {}
              (element :buildCommand {}
                       (element :name {} "ccw.builder")
                       (element :arguments))
              (element :buildCommand {}
                       (element :name {} "org.eclipse.jdt.core.javabuilder")
                       (element :arguments)))
     (element :natures {}
           (element :nature {} "ccw.nature")
           (element :nature {} "org.eclipse.jdt.core.javanature")))
   out-file))

(defn eclipse
  "Create Eclipse project descriptor files."
  [project]
  (with-open
      [out-file (file (:root project) ".classpath")]
    (create-classpath project out-file))
  (println "Created .classpath")
  (with-open
      [out-file (file (:root project) ".project")]
    (create-project project out-file))
  (println "Created .project"))
