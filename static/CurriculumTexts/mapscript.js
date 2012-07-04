$(document).ready(function(){

 var new_map='<span class="map"><span class="s1" /><span class="s2" /><span class="s3" /><span class="s4" /><span class="s5" /><span class="s6" /><span class="s7" /><span class="s8" /><span class="s9" /><span class="s10" /><span class="s11" /><span class="s12" /><span class="s13" /><span class="s14" /><span class="s15" /><span class="s16" /><span class="s17" /><span class="s18" /></span>';
 var new_bg=$("<span>");
 new_bg.addClass("bg");
 $("#europe li a").append(new_map);
 $("#europe li a").append(new_bg);

});