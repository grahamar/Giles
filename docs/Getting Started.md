# Getting Started with Giles

Giles is a [Play!](http://www.playframework.com/) application, written in Scala and therefore runs on the JVM.

## Requirements

- Giles is compiled with JDK6 so you'll need at least that for him to run.
- Giles uses [mongoDB](http://www.mongodb.org/) as his storage, so you'll need to have that setup first.*

You'll need the latest Giles distribution archive. After cloning Giles you can generate the distribution archive like
any other Play! application.

        play dist
    or
        play universal:package-zip-tarball

## Installing Giles

Once you have the latest Giles distribution archive you can unpack it to wherever you wish and run `bin/giles`

*See [Play! production configuration](http://www.playframework.com/documentation/2.2.x/ProductionConfiguration)
documentation for further details*
