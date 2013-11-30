# Configuring Giles

Giles doesn't have much configuration. All he needs to know is where he can clone your repositories to,
store your documents and keep his index.

## Repositories

Giles by defualt will clone your repositories to a directory named `.git_checkouts` underneath his root directory.
This can be configured using the `git.checkouts.dir` property in the `application.conf` file in the `conf` directory
underneath his root directory.

## Storage

Giles needs a mongoDB instance to store a few things such as users, projects, files, etc. Giles knows how to connect to
the your mongoDB instance by reading the `mongodb.yml` file, also in the `conf` directory underneath his root
directory.

## Index

Giles by default will store his search index in a directory named `.index` underneath his root directory. This can be
configured by the `index.dir` property in the `application.conf` file in the `conf` directory underneath his root
directory.
