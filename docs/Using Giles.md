# Using Giles

So far Giles only understands markdown (technically... see the [REST API](Rest API.md) for more details), don't worry
he's trying to learn other formats!
Other than that he's not fussy about what or where you keep your documentation (as long as it's under the root of your
repository... but of course you knew that).

### How to add a project to Giles

There's a few ways Giles can learn about a project. You can either import your project through the UI or through the
REST API. If you import your project through the UI, Giles will automatically checkout each tagged version of your
repository and the latest HEAD, and scan each one for markdown files and add any he finds.
If you add your project through the REST API, Giles will create a project record but he waits for you to send him files
via the REST API (unless the update web hook is triggered...).

*See the [REST API](Rest API.md) documentation for adding a project via REST*
Once you have signed in, either by creating an account or using your [Google OpenID](https://developers.google.com/accounts/docs/OpenID).
You can import a project from your dashboard page.

![](http://i.imgur.com/B255blE.png)

### PlantUML Support

For more details see: [PlantUML](http://plantuml.sourceforge.net)

Giles supports PlatUML embedded inside your markdown via the following syntax:

    <plantuml>
    Bob->Alice : hello
    </plantuml>

That's all there is to it...!