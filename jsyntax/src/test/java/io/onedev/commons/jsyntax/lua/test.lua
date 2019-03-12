require "hello2"  -- 包含hello2这个脚本  
   
-- 注视语句   
   
-- 基本上调用的cocos2dx函数和类的时候就是以cocos2d.*这样子来用  
-- 注意2：function 关键字定义函数，end结束函数  
   
-- 打印  
cocos2d.CCLuaLog("脚本hello开始运行... " .. myadd(3, 5))  
   
-- 创建一个Scene  
sceneForWorld = cocos2d.CCScene:node()  
   
-- 创建一个Layer  
layerForWorld = cocos2d.CCLayer:node()  
sceneForWorld:addChild(layerForWorld)  
   
-- 创建一个精灵  
spriteForWorld  = cocos2d.CCSprite:spriteWithFile("Icon.png")  
layerForWorld:addChild(spriteForWorld)  
   
-- 获取屏幕宽高  
screenSize=cocos2d.CCDirector:sharedDirector():getWinSize()  
   
-- 设置精灵坐标  
spriteForWorld:setPosition(cocos2d.CCPoint(screenSize.width*0.5,screenSize.height*0.5))  
   
-- 设置精灵缩放2倍  
spriteForWorld:setScale(2)  
   
-- 添加一个CCLabelTTF    （！！！！！！备注！！！！！！）  
myLableTTF =cocos2d.CCLabelTTF:labelWithString("Himi- Lua 基础","Helvetica-Bold",24)  
myLableTTF:setPosition(cocos2d.CCPoint(screenSize.width*0.5,screenSize.height*0.5+100))  
sceneForWorld:addChild(myLableTTF)  
-- 添加一个CCLabelTTF  
myLableTTF2 =cocos2d.CCLabelTTF:labelWithString("上面icon跟随用户触屏位置","Helvetica-Bold",24)  
myLableTTF2:setPosition(cocos2d.CCPoint(screenSize.width*0.5,screenSize.height*0.5-100))  
sceneForWorld:addChild(myLableTTF2)  
   
--   @@@@@@@@@@触摸事件  
   
--开启触摸  
layerForWorld:setIsTouchEnabled(true)  
   
-- 注册触摸事件  
layerForWorld.__CCTouchDelegate__:registerScriptTouchHandler(cocos2d.CCTOUCHBEGAN, "btnTouchBegin")  
layerForWorld.__CCTouchDelegate__:registerScriptTouchHandler(cocos2d.CCTOUCHMOVED, "btnTouchMove")  
layerForWorld.__CCTouchDelegate__:registerScriptTouchHandler(cocos2d.CCTOUCHENDED, "btnTouchEnd")  
   
-- touch handers  
pointBegin = nil  
   
function btnTouchBegin(e)  
    cocos2d.CCLuaLog("btnTouchBegin")  
    local v = e[1]  
    local pointMove = v:locationInView(v:view())  
    pointMove = cocos2d.CCDirector:sharedDirector():convertToGL(pointMove)  
    spriteForWorld:setPosition(cocos2d.CCPoint(pointMove.x,pointMove.y))  
end  
   
function btnTouchMove(e)  
    cocos2d.CCLuaLog("btnTouchMove")  
    local v = e[1]  
    local pointMove = v:locationInView(v:view())  
    pointMove = cocos2d.CCDirector:sharedDirector():convertToGL(pointMove)  
    spriteForWorld:setPosition(cocos2d.CCPoint(pointMove.x,pointMove.y))  
end  
   
function btnTouchEnd(e)  
    cocos2d.CCLuaLog("btnTouchEnd")  
end  
   
--   @@@@@@@@@@触摸结束  
   
--动态小狗  
winSize = cocos2d.CCDirector:sharedDirector():getWinSize()  
FrameWidth = 105  
FrameHeight = 95  
   
textureDog = cocos2d.CCTextureCache:sharedTextureCache():addImage("dog.png")  
frame0 = cocos2d.CCSpriteFrame:frameWithTexture(textureDog, cocos2d.CCRectMake(0, 0, FrameWidth, FrameHeight))  
frame1 = cocos2d.CCSpriteFrame:frameWithTexture(textureDog, cocos2d.CCRectMake(FrameWidth*1, 0, FrameWidth, FrameHeight))  
   
spriteDog = cocos2d.CCSprite:spriteWithSpriteFrame(frame0)  
spriteDog:setPosition(cocos2d.CCPoint(100, winSize.height/4*3))  
layerForWorld:addChild(spriteDog)  
   
animFrames = cocos2d.CCMutableArray_CCSpriteFrame__:new(2)  
animFrames:addObject(frame0)  
animFrames:addObject(frame1)  
   
animation = cocos2d.CCAnimation:animationWithFrames(animFrames, 0.5)  
   
animate = cocos2d.CCAnimate:actionWithAnimation(animation, false);  
spriteDog:runAction(cocos2d.CCRepeatForever:actionWithAction(animate))  
   
--自定义函数  
function prForHimi()  
    cocos2d.CCLuaLog("reFresh function")  
    --取消选择器  
    --cocos2d.CCScheduler:sharedScheduler():unscheduleScriptFunc("prForHimi")  
end  
   
--使用选择器进行函数更新  
--cocos2d.CCScheduler:sharedScheduler():scheduleScriptFunc("prForHimi", 1, false)  
   
--循环语句  
for i=0,4,1 do  
    for j=0,4,2 do  
        cocos2d.CCLuaLog("for loop",i)  
    end  
end  
   
-- 避免内存泄漏  
collectgarbage( "setpause", 100)  
collectgarbage( "setstepmul", 5000)  
   
-- 播放背景音乐  
--CocosDenshion.SimpleAudioEngine:sharedEngine():playBackgroundMusic("background.mp3", true)  
-- 播放音效  
--CocosDenshion.SimpleAudioEngine:sharedEngine():preloadEffect("effect1.wav")  
   
-- run整个scene  
cocos2d.CCDirector:sharedDirector():runWithScene(sceneForWorld)  
   
cocos2d.CCLuaLog("脚本hello正常执行结束... " .. myadd(3, 5)) 