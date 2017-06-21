安装 ： cordova plugin add 插件安装包绝对路径
卸载 ： cordova plugin remove com.carshop.scancarid  
环境 ： cordova环境为6.5.0

准备：安装本插件之前先安装下面插件
$ ionic cordova plugin add cordova-plugin-broadcaster
$ npm install --save @ionic-native/broadcaster

使用方法：
function(){
 this.broadcaster.addEventListener('didShow').subscribe((event) =>

 		//在此处执行跳转页面和传值   值为：event['data']
      console.log('这句话是在ionic 里面打印的:===='+event['data'])
    );

    cordova.plugins.scancarid.coolMethod(["获取车牌号"], function (success) {

    }, function (error) {
      alert("本地导出失败-exportPath:" + error);
    });
		
}