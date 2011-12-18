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

The keys of the value map can be either keywords or strings; if a keyword and
string of the same name are present, the string is preferred. (Why support
both? Keywords are more convenient to use in Clojure, but not all valid
Mustache keys can be made into keywords. Rather than force strings, Stencil
lets you use whichever will work better for you). 

    (render-string "Hi there, {{name}}."
                   {"name" "Donald" :name "Dick"})
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

By default, the template cache will keep a template in the cache for 5
seconds before it will decide to reload it. You can set the cache policy
manually using the function `set-cache-policy`. The argument to
`set-cache-policy` is a function that returns true if the cached item is still
valid and false if it should be reloaded. The argument is a cache entry data
structure; you should check the source of `stencil.loader` for specifics.
However, there are some cache policy functions that should cover most cases:

* `cache-forever` - This function can be used as a cache policy function that
will keep templates in the cache forever (or until they are explicitly
reloaded). This can be useful if you know that templates won't change during
the life of the program.
* `cache-never` - This function can be used similarly to never cache a
template. Could be useful for development, so that changes to templates are
available immediately.
* `cache-timeout` - This function cannot be used as a cache policy function
directly, it's a combinator. It takes an integer argument that will be the
number of milliseconds to keep an item in the cache, and returns a cache
policy function that implements that policy. The default cache policy is
`(cache-timeout 5000)`.

You can also work at an even lower-level, manually caching templates using the
`cache` function and the functions related to accessing the cache, then
calling `render` yourself. You should read the source for a better idea of
how to do that.

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

## Obtaining

If you are using Leiningen, you can add

    [stencil "0.2.0"]

to your project.clj and run `lein deps`.

## Bugs and Missing Features

I don't currently know of any bugs or issues with the software, but there
probably are some. If you run into anything, please let me know so I can fix
it as soon as possible.

## Recently

* Released version 0.2.0. Supports Clojure 1.3 and now builds with lein instead of cake. Now uses Slingshot for exceptions instead of clojure.contrib.condition; should not result in any code changes unless you are examining exceptions.

### Previously...

* Released version 0.1.2, fixing bug in the handling of missing partial templates and adding functions to remove entries from the dynamic template store and cache.

* Released version 0.1.1, fixing bug in the handling of inverted sections.

## License

Eclipse Public License
