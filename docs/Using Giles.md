# Using Giles

So far Giles only understands markdown (technically... see the [REST API](Rest API.md) for more details), don't worry
he's trying to learn other formats!
Other than that he's not fussy about what or where you keep your documentation (as long as it's under the root of your
repository... but of course you knew that).

### How does Giles work?

#### Parsing
Giles will crawl through your repository for any markdown files he can make sense of (*.md, *.markdown) and convert it
to HTML, using the [knockoff](https://github.com/tristanjuricek/knockoff) markdown parsing library for this.

#### Storing
Giles doesn't like storing more than 1 copy of any documentation file. So he hashes the generated HTML content using a
compound hash of SHA1 & MD5. Along with the compound hash, Giles uses the size of the file to tell if he's already got
an exact match of that file and therefore will not store it again but rather reference the stored copy. Giles also
compresses the stored HTML using the GZIP compression format.

#### Indexing
Giles indexes your documentation using [lucene](http://lucene.apache.org/) so you can search over every project he
knows of, or by project, or by project version.

### How to add a project to Giles

There's a few ways Giles can learn about a project. You can either import your project through the UI or through the
REST API. If you import your project through the UI, Giles will automatically checkout each tagged version of your
repository and the latest HEAD, and scan each one for markdown files and add any he finds.
If you add your project through the REST API, Giles will create a project record but he waits for you to send him files
via the REST API (unless the update web hook is triggered...).

*See the [REST API](Rest API.md) documentation for adding a project via REST*
Once you have signed in, either by creating an account or using your [Google OpenID](https://developers.google.com/accounts/docs/OpenID).
You can import a project from your dashboard page.

![](http://i.imgur.com/YQfdDBa.png)
