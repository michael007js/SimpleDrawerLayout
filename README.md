# SimpleDrawerLayout

No picture u say a j8!

少啰嗦，先看效果

![闭嘴看图](https://github.com/michael007js/SimpleDrawerLayout/blob/master/images/demo.gif "闭嘴看图")


## 项目介绍

这个抽屉菜单扩展了一些原生不支持的动画交互效果， 支持上下左右各个方向拉出，可当做上拉框用



重要的事情说三遍：
全程就一个文件！全程就一个文件！全程就一个文件！

[真·一个文件](https://github.com/michael007js/SimpleDrawerLayout/blob/master/app/src/main/java/com/sss/simpleDrawerLayout/SimpleDrawerLayout.java)


极大的降低耦合，想怎么折腾就怎么折腾

# 亮点：与系统的drawerLayout可以做到无缝对接，直接将drawerLayout替换成simpleDrawerLayout即可，其余方法逻辑不需要动

唯一的回调事件 ：

        setOnSimpleDrawerLayoutCallBack(new SimpleDrawerLayout.OnSimpleDrawerLayoutCallBack() {
            @Override
            public void onDrawerStatusChanged(SimpleDrawerLayout drawerLayout, int status, int amount, int state) {

            }
        });

常用状态介绍：

    public final static int OPENING = 4;//抽屉打开中
    public final static int OPENED = 5;//抽屉被打开
    public final static int CLOSING = 6;//抽屉关闭中
    public final static int CLOSED = 7;//抽屉被关闭
    
    public final static int START = 8;//抽屉开始移动
    public final static int MOVING = 9;//抽屉移动中
    public final static int END = 10;//抽屉移动结束
    
    
Api:

    /**
     * 设置抽屉方向
     */
    public void setGravity(int gravity)
    
    /**
     * 获取抽屉方向
     */
    public int getGravity()
    
    /**
     * 打开抽屉
     */
    public void openDrawer(int gravity)
    
    /**
     * 关闭抽屉
     */
    public void closeDrawers()
    
    /**
     * 设置抽屉尺寸（0f-1f）
     */
    public void setDrawerPercent(float drawerPercent) 
    
    /**
     * 获取抽屉的状态
     */
    public int getDrawerStatus()
    
    /**
     * 贝塞尔颜色是否依附抽屉背景色
     */
    public void setAttachDrawerColor(boolean attachDrawerColor)
    
    /**
     * 设置抽屉背景色
     */
    public void setBackgroundColor(@ColorInt int color)
    
    /**
     * 设置贝塞尔颜色
     */
    public void setBesselColor(int alpha, int red, int green, int blue)
  
 over

 By SSS





