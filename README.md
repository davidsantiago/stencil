# Stencil

A fast, compliant implementation of [Mustache](http://mustache.github.com)
in Clojure.

## Introduction

Stencil is a complete implementation of the 
[Mustache spec](http://github.com/mustache/spec), including the optional
lambdas.

The unit tests for Stencil will automatically pull down the spec
files using git and run the tests against the current implementation (If you
want to do this yourself, you can clone the repo and type `lein test`).
Currently, all spec tests are passing.

To learn about the language itself, you should read the language
[documentation](http://mustache.github.com). The rest of this document will
focus on the API that Stencil provides.

Like Mustache itself, the interface is very simple, consisting of two main
functions that will probably do most of what you want.

    (use 'stencil.core)
    (render-string "Hi there, {{name}}."
                   {:name "Donald"})
    "Hi there, Donald."

The easiest way to render a small template is using the function
`render-string`, which takes two arguments, a string containing the text of
the Mustache template and a map of the values referenced in the template.

The keys of the value map can be either keywords or strings; if a
keyword and string of the same name are present, the keyword is
preferred. (Why support both? Keywords are more convenient to use in
Clojure, but not all valid Mustache keys can be made into
keywords. Rather than force strings, Stencil lets you use whichever
will work better for you).

    (render-string "Hi there, {{name}}."
                   {"name" "Dick" :name "Donald"})
    "Hi there, Donald."

For a larger template, holding onto it and passing it in as a string is
neither the most convenient nor the fastest option. Most commonly, Mustache
templates are placed into their own files, ending in ".mustache", and put on
the app's classpath somewhere. In this case, the `render-file` function can
be used to open the file by its name and render it.

    (render-file "hithere"
                 {:name "Donald"})
    "Hi there, Donald."

The `render-file` function, given "hithere" as its first argument, will look
in the classpath for "hithere.mustache". If that is not found, it looks for
just the literal string itself, in this case "hithere". Remember that a
file-separating slash is perfectly fine to pull a file out of a subdirectory.

An important advantage that `render-file` has over `render-string` is that
the former will cache the results of parsing the file, and reuse the parsed
AST on subsequent renders, greatly improving the speed.

## Lower Level APIs

You can also manage things at a much lower level, if you prefer. In the
`stencil.loader` namespace are functions that Stencil itself uses the load
and cache templates. In particular, the function `load` will take a template
name and return the parsed AST out of cache if possible, and if not, it will
load and parse it. The AST returned from `load` can then be rendered with
the function `render`.

    (use 'stencil.loader)
    (render (load "hithere") {:name "Donald"})
    "Hi there, Donald."

At an even lower level, you can manually generate the AST used in rendering
using the function `parse` from the `stencil.parser` namespace. Of course,
doing it this way will bypass the cache entirely, but it's there if you want
it.

### Manual Cache Management

Stencil uses [core.cache](https://github.com/clojure/core.cache) for
caching. By default, Stencil uses a simple LRU cache. This is a pretty
good cache to use in deployed code, where the set of templates being
rendered is probably not going to change during runtime. However, you
can control the type of cache used by Stencil to get the most benefit
out of your specific code's usage patterns. You can set the cache
manually using the function `set-cache` from the `stencil.loader`
namespace; pass it some object that implements the `CacheProtocol`
protocol from core.cache. In particular, during development, you might
want to use a TTL cache with a very low TTL parameter, so that
templates are reloaded as soon as you modify them. For example:

    (stencil.loader/set-cache (clojure.core.cache/ttl-cache-factory {} :ttl 0))

You can also work at an even lower-level, manually caching templates using the
`cache` function and the functions related to accessing the cache, then
calling `render` yourself. You should read the source for a better idea of
how to do that.

#### Core.Cache Optional Mode (Experts only!)

You can also run Stencil without the core.cache dependency present. If
you don't have a really good reason for doing this, you almost
certainly don't want to do it! It's not a great idea, and it doesn't
provide any performance improvements or other benefits. It's actually
all drawbacks and degradations. Nonetheless, there are unlikely
scenarios where you might need to use Stencil this way to get by.

If you still think this is for you, you need to call
`stencil.loader/set-cache` with a "cache-like object" before you
attempt to use any Stencil functions, or you will get an error on any
use attempts. A plain map will work. Be aware, though, that if your
cache-like object is not actually a cache (ie, doesn't evict entries
once it reaches a size threshold of some sort), then it's quite
possible that this object will simply grow larger and larger in memory
over time without end, depending on how your code uses templates. Some
apps could get by in this situation (a command line app that runs once
and exits immediately, for example), while others might not.

### Manual Template Management

Sometimes it can be useful to refer to a template by name, even though that
template is not available as a file on the classpath. In that case, you can
register the template's source with Stencil, and later when you refer to that
template by its name, Stencil will check first to see if it is one that you
have manually registered, before checking the filesystem for it.

    (use 'stencil.loader)
    (register-template "hithere" "Hi there, {{name}}.")
    (render-file "hithere" {:name "Donald"})
    "Hi there, Donald."

## Performance

Performance isn't the most important thing in a template language, but I've
tried to make Stencil as fast as possible. In 
[basic tests](http://github.com/davidsantiago/mustachequerade), it
appears to be pretty fast. Of course, the actual performance of any given
template is dictated by many factors, especially the size of the template,
the amount and type of data it is given, and what types of operations are
performed by the template.

In particular, the Mustache spec specifies that the output of lambda tags
should not be cached, and so Stencil does not. Keep that in mind if you decide
to use them in your templates.

I'd like to thank YourKit for helping me keep Stencil fast.

YourKit is kindly supporting open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
<a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
<a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.

## Obtaining

Simply add

    [stencil "0.3.2"]

to the `:dependencies` key of your project.clj.

## Bugs and Missing Features

I don't currently know of any bugs or issues with the software, but there
probably are some. If you run into anything, please let me know so I can fix
it as soon as possible.

## Recently

* Released version 0.3.2.
  - Fixed a problem causing an infinite loop when attempting to parse a malformed set-delimiter tag.
  - Updated code to work with Clojure 1.5. (Thanks to @bmabey).

* Released version 0.3.1.
  - Update version of core.cache to one that fixes bugs.

* Released version 0.3.0. 
  - Performance improvements (Thanks YourKit!).
  - Keywords are now preferred over strings in contexts.
  - Change to using core.cache for more flexible and easier to use
    caching. API is slightly different, but only if you were managing
    cache policy manually (see above).
  - Lambdas that have `:stencil/pass-context` true in their metadata will be called with
    the current context as their second argument.

### Previously...

* Released version 0.2.0. Supports Clojure 1.3 and now builds with lein instead of cake. Now uses Slingshot for exceptions instead of clojure.contrib.condition; should not result in any code changes unless you are examining exceptions.

* Released version 0.1.2, fixing bug in the handling of missing partial templates and adding functions to remove entries from the dynamic template store and cache.

* Released version 0.1.1, fixing bug in the handling of inverted sections.

## License

Eclipse Public License
