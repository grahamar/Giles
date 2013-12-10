package util

import java.net.URL

import models._
import com.wordnik.swagger.core.util.ScalaJsonUtil
import com.wordnik.swagger.model.{ApiListing, ApiListingReference, ResourceListing}

object SwaggerUtil {

  def parse(url: URL): SwaggerResourceListing = {
    val listing: ResourceListing = ScalaJsonUtil.mapper.reader(classOf[ResourceListing]).readValue(url)
    val apilistings = listing.apis.map(parse(url, _))
    SwaggerResourceListing(listing.apiVersion, listing.swaggerVersion, apilistings, listing.authorizations, listing.info)
  }

  def parse(base: URL, ref: ApiListingReference): ApiListing =
    ScalaJsonUtil.mapper.reader(classOf[ApiListing]).readValue(new URL(base.toExternalForm+ref.path))

  def parseResource(resource: String): ResourceListing =
    ScalaJsonUtil.mapper.reader(classOf[ResourceListing]).readValue(resource)

  def parseApiListing(apiListing: String): ApiListing =
    ScalaJsonUtil.mapper.reader(classOf[ApiListing]).readValue(apiListing)

}
