(ns nginx.clojure.test-all
   (:use [clojure.test])
   (:require [clj-http.client :as client]
             [clojure.edn :as edn]))

(def ^:dynamic *host* "localhost")
(def ^:dynamic *port* "8080")
(def ^:dynamic *debug* false)

(defn debug-println [& args]
  (when (true? *debug*)
      (apply println args)))

(deftest ^{:remote true} test-naive-simple
  (testing "hello clojure"
           (let [r (client/get (str "http://" *host* ":" *port* "/clojure") {:coerce :unexceptional})
                 h (:headers r)]
             (debug-println r)
             (debug-println "=================hello clojure end=============================")
             (is (= 200 (:status r)))
             (is (= "Hello Clojure & Nginx!" (:body r)))
             (is (= "text/plain" (h "content-type")))
             (is (= "22" (h "content-length")))
             (is (.startsWith (h "server") "nginx-clojure")))))

(deftest ^{:remote true} test-headers
  (testing "simple headers"
           (let [r (client/get (str "http://" *host* ":" *port* "/headers") {:coerce :unexceptional})
                 h (:headers r)
                 b (-> r :body (edn/read-string))]
             (debug-println r)
             (debug-println "===============simple headers end =============================")
             (is (= 200 (:status r)))
             (is (= "e29b7ffb8a5325de60aed2d46a9d150b" (h "etag")))
             (is (= ["no-store" "no-cache"] (h "cache-control")))
             (is (.startsWith (h "server") "nginx-clojure"))
             (is (= "http" (b :scheme)))
             (is (= "/headers" (b :uri)))
             (is (= *port* (b :server-port)))))
  
  (testing "lowercase/uppercase headers"
           (let [r (client/get (str "http://" *host* ":" *port* "/loweruppercaseheaders") {:coerce :unexceptional, :headers {"My-Header" "mytest"}})
                 h (:headers r)
                 b (-> r :body (edn/read-string))]
             (debug-println r)
             (debug-println "===============lowercase/uppercase headers =============================")
             (is (= 200 (:status r)))
             (is (= "e29b7ffb8a5325de60aed2d46a9d150b" (h "etag")))
             (is (= ["no-store" "no-cache"] (h "cache-control")))
             (is (.startsWith (h "server") "nginx-clojure"))
             (is (= "http" (b :scheme)))
             (is (= "text/plain" (h "content-type")))
             (is (= "mytest" (b :my-header)))
             (is (= "/loweruppercaseheaders" (b :uri)))
             (is (= *port* (b :server-port)))))
  
  (testing "cookie & user defined headers"
           (let [r (client/get (str "http://" *host* ":" *port* "/headers") {:coerce :unexceptional, :headers {"my-header" "mytest"}, :cookies {"tc1" {:value "tc1value"}, "tc2" {:value "tc2value"} } })
                 h (:headers r)
                 b (-> r :body (edn/read-string))]
             (debug-println r)
             (debug-println "===============cookie & user defined headers end=============================")
             (is (= 200 (:status r)))
             (is (= "e29b7ffb8a5325de60aed2d46a9d150b" (h "etag")))
             (is (= ["no-store" "no-cache"] (h "cache-control")))
             (is (.startsWith (h "server") "nginx-clojure"))
             (is (= "http" (b :scheme)))
             (is (= "/headers" (b :uri)))
             (is (= *port* (b :server-port)))
             (is (= "mytest" (b :my-header)))
             (is (= "tc1=tc1value;tc2=tc2value" (b :cookie)))))
  
    (testing "query string & character-encoding"
           (let [r (client/get (str "http://" *host* ":" *port* "/headers?my=test") {:coerce :unexceptional, :headers {"my-header" "mytest" "Content-Type" "text/plain; charset=utf-8"}, :cookies {"tc1" {:value "tc1value"}, "tc2" {:value "tc2value"} } })
                 h (:headers r)
                 b (-> r :body (edn/read-string))]
             (debug-println r)
             (debug-println "===============query string & character-encoding =============================")
             (is (= 200 (:status r)))
             (is (= "e29b7ffb8a5325de60aed2d46a9d150b" (h "etag")))
             (is (= ["no-store" "no-cache"] (h "cache-control")))
             (is (.startsWith (h "server") "nginx-clojure"))
             (is (= "http" (b :scheme)))
             (is (= "/headers" (b :uri)))
             (is (= *port* (b :server-port)))
             (is (= "mytest" (b :my-header)))
             (is (= "tc1=tc1value;tc2=tc2value" (b :cookie)))
             (is (= "my=test" (b :query-string)))
             (is (= "utf-8" (b :character-encoding))))))

(deftest ^{:remote true} test-form
  (testing "form method=get"
           (let [r (client/get (str "http://" *host* ":" *port* "/form") {:coerce :unexceptional, :query-params {:foo "bar"}})
                 h (:headers r)
                 b (-> r :body (edn/read-string))]
             (debug-println r)
             (debug-println "=================form method=get end=============================")
             (is (= 200 (:status r)))
             (is (= "foo=bar" (b :query-string)))
             (is (nil? (b :form-body-str)))
             ))
  (testing "form method=post"
           (let [r (client/post (str "http://" *host* ":" *port* "/form") {:coerce :unexceptional, :form-params {:foo "bar"}})
                 h (:headers r)
                 b (-> r :body (edn/read-string))]
             (debug-println r)
             (debug-println "=================form method=post end=============================")
             (is (= 200 (:status r)))
             (is (= "foo=bar" (b :form-body-str)))
             (is (= "nginx.clojure.NativeInputStream" (b :form-body-type)))
             (is (nil? (b :query-string)))
             ))
  (testing "form multipart-formdata"
           (let [r (client/post (str "http://" *host* ":" *port* "/echoUploadfile") {:coerce :unexceptional, :multipart [{:name "mytoken", :content "123456"},
                                                                                                 {:name "myf", :content (clojure.java.io/file "test/nginx-working-dir/post-test-data")}
                                                                                                 ]})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================form multipart-formdata=============================")
             (is (= 200 (:status r)))
             (is (< 0 (.indexOf b "name=\"mytoken\"")))
             (is (< 0 (.indexOf b "123456")))
             (is (< 0 (.indexOf b "name=\"myf\"")))
             (is (< 0 (.indexOf b "Apache HTTP Server Version 2.4")))
             (is (< 0 (.indexOf b "Modules | Directives | FAQ | Glossary | Sitemap")))
             ))
  )


(deftest ^{:remote true} test-file
  (testing "static file without gzip"
           (let [r (client/get (str "http://" *host* ":" *port* "/files/small.html") {:coerce :unexceptional, :decompress-body false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================static file (no gzip) end =============================")
             (is (= 200 (:status r)))
             (is (= "680" (h "content-length")))))
    (testing "static file with gzip"
           ;clj-http will auto use Accept-Encoding	gzip, deflate
           (let [r (client/get (str "http://" *host* ":" *port* "/files/small.html"))
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================static file (with gzip) end=============================")
             (is (= 200 (:status r)))
             (is (= "gzip" (:orig-content-encoding r)))
             (is (= 680 (count (r :body))))))
    
   (testing "static file with range operation"
      ;clj-http will auto use Accept-Encoding	gzip, deflate
      (let [r (client/get (str "http://" *host* ":" *port* "/files/small.html")  {:coerce :unexceptional, :decompress-body false, :headers {"Range" "bytes=0-128"}})
            h (:headers r)
            b (r :body)]
        (debug-println r)
        (debug-println "=================static file (with range 0-128) end=============================")
        ;206 Partial Content
        (is (= 206 (:status r)))
        (is (= 129 (count (r :body))))))
    
  )

(deftest ^{:remote true} test-seq
  (testing "seq include String &  File without gzip"
           (let [r (client/get (str "http://" *host* ":" *port* "/testMySeq") {:coerce :unexceptional, :decompress-body false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================seq include String &  File without gzip=============================")
             (is (= 200 (:status r)))
             (is (= (str (+ 680 (count "header line\n"))) (h "content-length")))))
  )


(deftest ^{:remote true} test-inputstream
  (testing "inputstream without gzip"
           (let [r (client/get (str "http://" *host* ":" *port* "/testInputStream") {:coerce :unexceptional, :decompress-body false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================inputstream (no gzip) end =============================")
             (is (= 200 (:status r)))
             (is (= "680" (h "content-length")))))
    (testing "inputstream with gzip"
           ;clj-http will auto use Accept-Encoding	gzip, deflate
           (let [r (client/get (str "http://" *host* ":" *port* "/testInputStream"))
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================inputstream (with gzip) end=============================")
             (is (= 200 (:status r)))
             (is (= "gzip" (:orig-content-encoding r)))
             (is (= 680 (count (r :body))))))
  )

(deftest ^{:remote true} test-redirect
  (testing "redirect"
           (let [r (client/get (str "http://" *host* ":" *port* "/testRedirect") {:follow-redirects false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================redirect=============================")
             (is (= 302 (:status r)))
             (is (= (str  "http://" *host* ":" *port*  "/files/small.html") (h "location"))))))


(deftest ^{:remote true} test-ring-compojure
    (testing "hello"
           (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/hello") {:throw-exceptions false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure hello=============================")
             (is (= 200 (:status r)))
             (is (= "text/plain" (h "content-type")))
             (is (= "Hello World" b))))
    (testing "redirect"
           (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/redirect") {:follow-redirects false :throw-exceptions false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure redirect=============================")
             (is (= 302 (:status r)))
             (is (= "http://example.com" (h "location")))))
    (testing "file-response"
           (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/file-response" ) {:throw-exceptions false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure file-response=============================")
             (is (= 200 (:status r)))
             ;(is (= "gzip" (:orig-content-encoding r)))
             (is (= 680 (count (r :body))))))
    (testing "resource-response"
           (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/resource-response") {:throw-exceptions false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure resource-response=============================")
             (is (= 200 (:status r)))
             ;(is (= "gzip" (:orig-content-encoding r)))
             (is (= 680 (count (r :body))))))
    (testing "wrap-content-type"
           (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/wrap-content-type.html") {:throw-exceptions false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure wrap-content-type=============================")
             (is (= 200 (:status r)))
             (is (= "text/x-foo" (h "content-type")))))
    (testing "wrap-params"
           (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/wrap-params?x=hello&x=world") {:throw-exceptions false})
                 h (:headers r)
                 params (-> "rmap" h (edn/read-string) :params)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure wrap-params=============================")
             (is (= 200 (:status r)))
             ;(is (= "gzip" (:orig-content-encoding r)))
             (is (= ["hello", "world"] (params "x")))))
    (testing "wrap-params-post"
           (let [r (client/post (str "http://" *host* ":" *port* "/ringCompojure/wrap-params" ) {:coerce :unexceptional, :form-params {:foo "bar"}, :throw-exceptions false})
                 h (:headers r)
                 params (-> "rmap" h (edn/read-string) :params)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure wrap-params-post=============================")
             (is (= 200 (:status r)))
             ;(is (= "gzip" (:orig-content-encoding r)))
             (is (= "bar" (params "foo")))))
    (testing "wrap-cookies"
           (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/wrap-cookies") {:coerce :unexceptional,:cookies {"tc1" {:value "tc1value"}, "tc2" {:value "tc2value"} }, :throw-exceptions false})
                 h (:headers r)
                 cookies (-> "rmap" h (edn/read-string) :cookies)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure wrap-cookies=============================")
             (is (= 200 (:status r)))
             ;(is (= "gzip" (:orig-content-encoding r)))
             (is (= {"tc1" {:value "tc1value"}, "tc2" {:value "tc2value"} } cookies))))
    (testing "authorized-service"
             (let [
                   r1 (client/get (str "http://" *host* ":" *port* "/ringCompojure/authorized-service") {:coerce :unexceptional, :throw-exceptions false})
                   r2 (client/get (str "http://" *host* ":" *port* "/ringCompojure/authorized-service") {:basic-auth ["nginx-clojure" "xxxx"] :coerce :unexceptional, :throw-exceptions false})
                   ]
               (is (= 401 (:status r1)))
               (is (= 200 (:status r2)))))
    (testing "json-patch"
             (let [msg "{\"value\": 5}"
                   r (client/patch (str "http://" *host* ":" *port* "/ringCompojure/json-patch") {:coerce :unexceptional, :throw-exceptions false, :body msg})
                   ]
               (is (= (str "Your patch succeeded! length=" (count msg)) (:body r)))))
    (testing "wrap-session"
           (let [cs (clj-http.cookies/cookie-store)]
             (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/wrap-session") {:throw-exceptions false, :cookie-store cs})
                   b (r :body)]
             (debug-println r)
             (debug-println cs)
             (debug-println "=================test-ring-compojure wrap-session 1=============================")
             (is (= 200 (:status r)))
             (is (= "Welcome guest!" b)))
             (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/wrap-session?user=Tom") {:throw-exceptions false, :cookie-store cs})
                   b (r :body)]
             (debug-println r)
             (debug-println cs)
             (debug-println "=================test-ring-compojure wrap-session 2=============================")
             (is (= 200 (:status r)))
             (is (= "Welcome Tom!" b)))
             (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/wrap-session") {:throw-exceptions false, :cookie-store cs})
                   b (r :body)]
             (debug-println r)
             (debug-println cs)
             (debug-println "=================test-ring-compojure wrap-session 3=============================")
             (is (= 200 (:status r)))
             (is (= "Welcome Tom!" b)))
             )
           )
    (testing "not-found"
           (let [r (client/get (str "http://" *host* ":" *port* "/ringCompojure/not-found") {:throw-exceptions false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================test-ring-compojure hello=============================")
             (is (= 404 (:status r)))
             (is (= "text/html; charset=utf-8" (h "content-type")))
             (is (= "<h1>Page not found</h1>" b))))    
  )

(deftest ^{:async true :remote true} test-asyncsocket
    (let [
        ;r1 (client/get "http://mirror.bit.edu.cn/apache/httpcomponents/httpclient/RELEASE_NOTES-4.3.x.txt")
        ;b1 (r1 :body)
        abc ""
        ]
      (testing "asyncsocket --simple example"
           (let [r (client/get (str "http://" *host* ":" *port* "/asyncsocket") {:throw-exceptions false})
                 h (:headers r)
                 b (r :body)
                 bb (subs b (.indexOf b "\r\n\r\n"))
                 sf (nginx.clojure.net.SimpleHandler4TestNginxClojureSocket.)
                 r1 (sf {})
                 b1 (slurp (r1 :body))
                 b1b (subs b1 (.indexOf b1 "\r\n\r\n"))]
             (debug-println "=================asyncsocket simple example =============================")
             (is (= 200 (:status r)))
             (is (= (.length bb) (.length b1b)))
             (is (= bb b1b))))
    )
  
  )

(deftest ^{:async true :remote true} test-cljasyncsocket
    (let [
        ;r1 (client/get "http://mirror.bit.edu.cn/apache/httpcomponents/httpclient/RELEASE_NOTES-4.3.x.txt")
        ;b1 (r1 :body)
        abc ""
        ]
      (testing "asyncsocket --simple example"
           (let [r (client/get (str "http://" *host* ":" *port* "/cljasyncsocket") {:throw-exceptions false})
                 h (:headers r)
                 b (r :body)
                 bb (subs b (.indexOf b "\r\n\r\n"))
                 sf (nginx.clojure.net.SimpleHandler4TestNginxClojureSocket.)
                 r1 (sf {})
                 b1 (slurp (r1 :body))
                 b1b (subs b1 (.indexOf b1 "\r\n\r\n"))]
             (debug-println "=================clj asyncsocket simple example =============================")
             (is (= 200 (:status r)))
             (is (= (.length bb) (.length b1b)))
             (is (= bb b1b))))
    )
  
  )

;(comment 
(deftest ^{:remote true} test-coroutine
  (let [r1 (client/get "http://mirror.bit.edu.cn/apache/httpcomponents/httpclient/RELEASE_NOTES-4.3.x.txt")
        b1 (r1 :body)]
      (testing "coroutine based socket--simple example"
           (let [r (client/get (str "http://" *host* ":" *port* "/socket") {:throw-exceptions false})
                 h (:headers r)
                 b (r :body)
                 bb (subs b (.indexOf b "\r\n\r\n"))
                 sf (nginx.clojure.net.SimpleHandler4TestNginxClojureSocket.)
                 r1 (sf {})
                 b1 (slurp (r1 :body))
                 b1b (subs b1 (.indexOf b1 "\r\n\r\n"))]
             (debug-println "=================coroutine based socket simple example =============================")
             (is (= 200 (:status r)))
             (is (= (.length bb) (.length b1b)))
             (is (= bb b1b))))

     (testing "coroutine based socket--httpclient get"
              (let [r (client/get (str "http://" *host* ":" *port* "/httpclientget") {:throw-exceptions false})
                    h (:headers r)
                    b (r :body)
;                 r1 (client/get "http://www.apache.org/dist/httpcomponents/httpclient/RELEASE_NOTES-4.3.x.txt")
;                 b1 (r1 :body)
                ]
             (debug-println "=================coroutine based socket httpclient get =============================")
             (is (= 200 (:status r)))
             (is (= (.length b) (.length b1)))
             (is (= b b1))))
     
     (testing "coroutine based socket--compojure & httpclient get"
            (let [r (client/get (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/simple-httpclientget") {:throw-exceptions false})
                  h (:headers r)
                  b (r :body)
;                 r1 (client/get "http://www.apache.org/dist/httpcomponents/httpclient/RELEASE_NOTES-4.3.x.txt")
;                 b1 (r1 :body)
                ]
             (debug-println "=================coroutine based socket compojure & httpclient get =============================")
             (is (= 200 (:status r)))
             (is (= (.length b) (.length b1)))
             (is (= b b1))))  
    ;http://localhost:8080/coroutineSocketAndCompojure/simple-clj-http-test
     (testing "coroutine based socket--compojure & clj-http get"
            (let [r (client/get (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/simple-clj-http-test") {:throw-exceptions false})
                  h (:headers r)
                  b (r :body)
;                 r1 (client/get "http://www.apache.org/dist/httpcomponents/httpclient/RELEASE_NOTES-4.3.x.txt")
;                 b1 (r1 :body)
                ]
             (debug-println "=================coroutine based socket compojure & clj-http get =============================")
             (is (= 200 (:status r)))
             (is (= (.length b) (.length b1)))
             (is (= b b1)))) 
    ;http://localhost:8080/coroutineSocketAndCompojure/fetch-two-pages
     (testing "coroutine based socket--co-pvalues & compojure & clj-http "
            (let [r (client/get (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/fetch-two-pages") {:throw-exceptions false})
                  h (:headers r)
                  b (r :body)
                 [r1, r2] (pvalues (client/get "http://mirror.bit.edu.cn/apache/httpcomponents/httpclient/")
                                   (client/get "http://mirror.bit.edu.cn/apache/httpcomponents/httpcore/"))
                 b12 (str (:body r1) "\n==========================\n" (:body r2))
                ]
             (debug-println "=================coroutine based socket--co-pvalues & compojure & clj-http  =============================")
             (is (= 200 (:status r)))
             (is (= (.length b) (.length b12)))
             (is (= b b12))))       
     (testing "coroutine based socket--compojure & mysql jdbc"
            (let [cr (client/get (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/mysql-create") {:throw-exceptions false})
                  ir1 (client/put (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/mysql-insert") {:form-params {:name "java" :rank "5"} :throw-exceptions false})
                  ir2 (client/put (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/mysql-insert") {:form-params {:name "clojure" :rank "4"} :throw-exceptions false})
                  ir3 (client/put (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/mysql-insert") {:form-params {:name "c" :rank "5"} :throw-exceptions false})
                  qr1 (client/get (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/mysql-query/java") {:throw-exceptions false })
                  qr2 (client/get (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/mysql-query/clojure") {:throw-exceptions false})
                  dr  (client/get (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/mysql-drop") {:throw-exceptions false})
                  qad (client/get (str "http://" *host* ":" *port* "/coroutineSocketAndCompojure/mysql-query/java") {:throw-exceptions false})
                ]
             (debug-println "=================coroutine based socket compojure & mysql jdbc- created=============================")
             (is (= 200 (:status cr)))
             (is (= "created!" (:body cr)))
             (is (= 200 (:status ir1)))
             (is (= "inserted!" (:body ir1)))
             (is (= 200 (:status ir2)))
             (is (= "inserted!" (:body ir2)))
             (is (= 200 (:status ir3)))
             (is (= "inserted!" (:body ir3)))
             (is (= 200 (:status qr1)))
             (is (= [{:name "java" :rank "5"}]  (-> qr1 :body (edn/read-string))))
             (is (= 200 (:status qr2)))
             (is (= [{:name "clojure" :rank "4"}] (-> qr2 :body (edn/read-string))))
             (is (= 200 (:status dr)))
             (is (= "dropped!" (:body dr)))
             (is (= 500 (:status qad)))
             ))       
     
    )
  )
;)

(deftest ^{:remote true} test-nginx-var
  (testing "simple nginx var"
           (let [r (client/get (str "http://" *host* ":" *port* "/vartest") {:follow-redirects false})
                 h (:headers r)
                 b (r :body)]
             (debug-println r)
             (debug-println "=================vartest=============================")
             (is (= 200 (:status r)))
             (is (= "Hello,Xfeep!" (:body r))))))

;eg. (concurrent-run 10 (run-tests 'nginx.clojure.test-all))
(defmacro concurrent-run 
  [n, form]
  (list 'apply 'pcalls (list 'repeat n (list 'fn [] form)) ))

;(binding [*host* "macosx"] (concurrent-test 4))
;(binding [*host* "cxp"] (concurrent-test 4))
(defn concurrent-test
  [n]
  (concurrent-run n (run-tests 'nginx.clojure.test-all)))




