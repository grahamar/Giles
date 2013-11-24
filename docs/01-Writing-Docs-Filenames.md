Getting Started
========

RTM was made to be as simple as possible to start using for your project's documentation.

### Writting Your Docs

Create a "docs" directory at the root of your project (this is configurable).

```
$ cd /path/to/project
$ mkdir docs
```

Documentation is proccessed by the [Pamflet](https://github.com/n8han/pamflet) application.
Pamflet read source files from the docs directory and produces the finished HTML files for
RTM to display. Files with a `markdown` or `md` extension correspond to one HTML output file.

Filenames are used only for ordering pages and sections; the display
titles and page URLs are determined instead by the first heading
element in the source of the page.

### Naming Conventions

It is suggested that you name your sources with a simple numeric or
alphabetical prefix to control ordering; after that you can include
some portion of the title, or nothing, and end with the markdown
extension.

For example, the title page of a pamflet could be named `00.markdown`
to ensure that is the first source in the ordered list.

#### Output Names

The output `html` filenames are URL-encoded versions of the page
names; the have no relationship to the input filenames. Page names
must therefore be unique across the pamflet.

