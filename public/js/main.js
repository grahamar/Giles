$(document).ready(function(){
  $('img.favourite').click(function(e){favourite(e, this)});
  $('img.unfavourite').click(function(e){unfavourite(e, this)});

  function favourite(e, link) {
    e.preventDefault();
    var projectUrlKey = $(link).attr('data-project');
    jQuery.ajax({
        url: jsRoutes.controllers.ProjectController.favouriteProject(projectUrlKey).url,
        type:'POST',
        async: true,
        cache: true,
        timeout: 10000,
        context: link,
        success: function(value){
           jQuery(link).toggleClass('unfavourite favourite');
           jQuery(link).attr("src", "/assets/img/star.png");
           jQuery(link).unbind("click")
           jQuery(link).bind("click", function(e){unfavourite(e, this)});
        },
        error: function() {
            //alert(error);
        }
    });
    return false; // prevent default
  }

  function unfavourite(e, link) {
    e.preventDefault();
    var projectUrlKey = $(link).attr('data-project');
    jQuery.ajax({
        url: jsRoutes.controllers.ProjectController.unfavouriteProject(projectUrlKey).url,
        type:'POST',
        async: true,
        cache: true,
        timeout: 10000,
        context: link,
        success: function(value){
           jQuery(link).toggleClass('favourite unfavourite');
           jQuery(link).attr("src", "/assets/img/star-empty.png");
           jQuery(link).unbind("click")
           jQuery(link).bind("click", function(e){favourite(e, this)});
        },
        error: function() {
            //alert(error);
        }
    });
    return false; // prevent default
  }
});
