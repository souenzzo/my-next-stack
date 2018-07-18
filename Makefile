#!/usr/bin/env make

resources/public/js/app.js: dist/index_bundle.js
	lein with-profile +client run -m cljs.main -co resources/build.edn -O advanced -v -c

dist/index_bundle.js: node_modules
	yarn webpack

node_modules:
	yarn install

