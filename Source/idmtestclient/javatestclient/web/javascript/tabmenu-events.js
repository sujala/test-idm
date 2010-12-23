var menuItems=["menu_customers", "menu_clients", "menu_users", "menu_password","menu_tokens"];

$(document).ready(function(){
	$(".menu > li").click(function(e){
		setLeftMenu(e.target.id);
		//alert(e.target.id);
		return false;
	});
});

function setLeftMenu(targetId) {
	for(var i=0, len=menuItems.length; i<len;i++) {
		var curItem = menuItems[i]; 
		if (curItem == targetId) {
			$("#" + curItem).addClass("active");
			$("div." + curItem).fadeIn();
		}
		else {
			$("#" + curItem).removeClass("active");
			$("div." + curItem).css("display", "none");
		}
	}
}