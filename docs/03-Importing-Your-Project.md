Importing Your Project
=========

Sign up for an account and log in. Visit your dashboard and click "Import project" to add your project
to the site. Fill in the name, then specify where your repository is located. This is normally the URL
you'd use to checkout, clone your code (NOTE: Please use the HTTP URL and not SSH). Example:

* Git: `http://github.com/grahamar/example-rtm-project.git`

For the "Default Branch" you'll need to enter the branch name of the active HEAD or master branch,
i.e. the branch for which the URL /latest should point to.

For the "Default Version" you'll need to enter the version name you'd like to use for the active
HEAD or master branch.

Typically you can just enter "master" for the default branch and "latest" for the default version.

Within a few minutes your code will automatically be fetched from your public repository, and the
documentation will be built for each tag in your repository including the HEAD/latest revision.
