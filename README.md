# re-frame Game of Life

A [re-frame](https://github.com/Day8/re-frame) front-end to visualize and interact
with Conway Game of Life ([Wikipedia](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life)).

The aim of this application is to explore re-frame and learn how code in
re-frame should be organized and structured. For this purpose, the following
interactions are added to the application:

* Run simulation
* Pause simulation
* Step through simulation
* Edit interval between each generation
* Edit the seed of the simulation

re-frame uses [Reagent](https://github.com/reagent-project/reagent), so this
application is also used to experiment with Reagent components to display
Game of Life on [HTML5 SVG](https://www.w3schools.com/Html/html5_svg.asp) and 
on [HTML5 Canvas](https://www.w3schools.com/Html/html5_canvas.asp).  
 
See live demo [here](https://saicheong.github.io/reframe-game-of-life/index.html).

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

### Fixing Extern Issues

In :advanced mode, the application will hit the following error when moving 
the mouse over the game grid. 

```bash
Uncaught TypeError: d.cd is not a function
    at app.js:478
    at app.js:479
    at Object.r (app.js:40)
    at a (app.js:38)
```
In :advanced mode, the Closure Compiler is used to optimized the application.
It does this by shortening (or "munging") variable names throughout the entire 
application. But when it does not have access to the whole program, it may end
up changing the name of external variables and functions is does not know about,
or changing the name of variables and functions referenced by external files
that it does not know about.

The following articles are very helpful in explaining the extern issue:
* [Externs: The Bane of every Release Build](https://code.thheller.com/blog/shadow-cljs/2017/10/15/externs-the-bane-of-every-release-build.html)
* [Using JavaScript libraries in ClojureScript](http://lukevanderhart.com/2011/09/30/using-javascript-and-clojurescript.html)

To protect variables and functions from being renamed, you need to define the
variables and functions in an extern file. Alternatively, you can let Clojurescript
[infer the externs](https://clojurescript.org/guides/externs) or notify you of externs
to be included.

To enable externs inference, specify the :infer-externs true in the compiler configuration:
```clojure
{:id "dev"
     ...
     :compiler {... 
                :optimizations :none
                :infer-externs true
                ...}
     ...}
```

Next set up the compiler to warn whenever it cannot determine the types involved in a dot form, 
whether property access or method invocation:

```clojure
(set! *warn-on-infer* true)
```
This needs to be set in every file that requires externs inference.

When you run the compiler, you get a list of warnings of externs and types involved in 
a dot form. 

```bash
~/coding/projects/conway$ lein cljsbuild once min
Compiling ClojureScript...
Compiling ["resources/public/js/compiled/app.js"] from ["src/cljs"]...
WARNING: Cannot infer target type in expression (. ev -clientX) at line 9 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. ev -clientY) at line 9 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. svg-elem createSVGPoint) at line 13 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. svg-elem getScreenCTM) at line 14 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. (.getScreenCTM svg-elem) inverse) at line 14 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. svg-point -x) at line 15 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. svg-point -y) at line 16 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. svg-point matrixTransform matrix) at line 17 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. gpt -x) at line 18 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. gpt -y) at line 18 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. elem getBoundingClientRect) at line 121 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. rect -left) at line 122 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. rect -top) at line 123 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. (clojure.core/deref elem) getContext "2d") at line 139 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. c fillRect (* x sz) (* y sz) wd wd) at line 147 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. ctx -fillStyle) at line 150 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. ctx -fillStyle) at line 155 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
Jul 05, 2018 10:32:01 PM com.google.javascript.jscomp.LoggerErrorManager println
WARNING: target/cljsbuild-compiler-1/inferred_externs.js:17: WARNING - name goog is not defined in the externs.
goog.DEBUG;
^^^^

Jul 05, 2018 10:32:01 PM com.google.javascript.jscomp.LoggerErrorManager println
WARNING: target/cljsbuild-compiler-1/inferred_externs.js:18: WARNING - name goog is not defined in the externs.
goog.isArrayLike;
^^^^

Jul 05, 2018 10:32:01 PM com.google.javascript.jscomp.LoggerErrorManager printSummary
WARNING: 0 error(s), 2 warning(s)
Successfully compiled ["resources/public/js/compiled/app.js"] in 12.783 seconds.
```

It is a long list of warnings. There are also 3 other warnings on the name goog
which should not be there since it is automatically included by Clojurescript

It may not be easy to tell which object needs to be added to externs. But in this
case, I happen to know (using pseudo-names option) that createSVGPoint was renamed - 
so I will add a hint there.

After adding type hint like so:
```clojure
(defn- svg-pos [client-pos ^js/SVGElement svg-elem]
  ...)
```

The methods associcated with SVG element were included in the externs.
Note that it is not important to get the type right. Using ^js/Object works as well.

```bash
~/coding/projects/conway$ lein cljsbuild once min
Compiling ClojureScript...
Compiling ["resources/public/js/compiled/app.js"] from ["src/cljs"]...
WARNING: Cannot infer target type in expression (. ev -clientX) at line 9 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. ev -clientY) at line 9 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Adding extern to Object for property createSVGPoint due to ambiguous expression (. svg-elem createSVGPoint) at line 13 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot resolve property createSVGPoint for inferred type js/Object in expression (. svg-elem createSVGPoint) at line 13 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Adding extern to Object for property getScreenCTM due to ambiguous expression (. svg-elem getScreenCTM) at line 14 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot resolve property getScreenCTM for inferred type js/Object in expression (. svg-elem getScreenCTM) at line 14 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Adding extern to Object for property inverse due to ambiguous expression (. (.getScreenCTM svg-elem) inverse) at line 14 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Adding extern to Object for property x due to ambiguous expression (. svg-point -x) at line 15 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Adding extern to Object for property y due to ambiguous expression (. svg-point -y) at line 16 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Adding extern to Object for property matrixTransform due to ambiguous expression (. svg-point matrixTransform matrix) at line 17 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Adding extern to Object for property x due to ambiguous expression (. gpt -x) at line 18 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Adding extern to Object for property y due to ambiguous expression (. gpt -y) at line 18 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. elem getBoundingClientRect) at line 121 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. rect -left) at line 122 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. rect -top) at line 123 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. (clojure.core/deref elem) getContext "2d") at line 139 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. c fillRect (* x sz) (* y sz) wd wd) at line 147 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. ctx -fillStyle) at line 150 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
WARNING: Cannot infer target type in expression (. ctx -fillStyle) at line 155 /Users/saicheong/Programming/projects/conway/src/cljs/conway/grid.cljs
Jul 05, 2018 10:49:37 PM com.google.javascript.jscomp.LoggerErrorManager println
WARNING: target/cljsbuild-compiler-1/inferred_externs.js:23: WARNING - name goog is not defined in the externs.
goog.DEBUG;
^^^^

Jul 05, 2018 10:49:37 PM com.google.javascript.jscomp.LoggerErrorManager println
WARNING: target/cljsbuild-compiler-1/inferred_externs.js:24: WARNING - name goog is not defined in the externs.
goog.isArrayLike;
^^^^

Jul 05, 2018 10:49:37 PM com.google.javascript.jscomp.LoggerErrorManager printSummary
WARNING: 0 error(s), 2 warning(s)
Successfully compiled ["resources/public/js/compiled/app.js"] in 13.505 seconds.
```

Note: There are some warnings that shouldn't be there. See:
https://dev.clojure.org/jira/browse/CLJS-2392

By using infer-externs option, and adding type hint, the :advanced mode can now work. 

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```

## License

Copyright © 2018 Yow Sai Cheong

Released under the MIT license.