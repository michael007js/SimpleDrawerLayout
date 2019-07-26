package com.sss.simpleDrawerLayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.text.DecimalFormat;

/**
 * 一个简单的抽屉菜单，扩展了一些原生不支持的交互效果
 */
public class SimpleDrawerLayout extends ViewGroup {
    /***************************以下为内部状态*****************************/
    private final static int NONE = 0;//初始状态，没有被操作过
    private final static int PREPARE = 1;//预打开
    private final static int DRAG = 2;//拖动
    private final static int OUT = 3;//贝塞尔退场

    /***************************以下为抽屉状态*****************************/
    public final static int OPENING = 4;//抽屉打开中
    public final static int OPENED = 5;//抽屉被打开
    public final static int CLOSING = 6;//抽屉关闭中
    public final static int CLOSED = 7;//抽屉被关闭
    public final static int START = 8;//抽屉开始移动
    public final static int MOVING = 9;//抽屉移动中
    public final static int END = 10;//抽屉移动结束

    public final static int BACKGROUND_COLOR = Color.parseColor("#ededed");//抽屉默认背景


    //第一次的触摸x点
    private int x = 0;
    //第一次的触摸y点
    private int y = 0;
    //实时的贝塞尔x点
    private int besselX = 0;
    //实时的贝塞尔y点
    private int besselY = 0;
    //方向
    private int gravity = Gravity.RIGHT;
    //当前内部状态
    private int status = NONE;
    //当前抽屉状态
    private int drawerStatus = CLOSED;
    //贝塞尔路径
    private Path besselPath = new Path();
    //贝塞尔画笔
    private Paint besselPaint = new Paint();
    //拉出抽屉菜单所触发的阀值
    private int triggerThreshold = dp2px(50);
    //贝塞尔阀值
    private int besselThreshold = triggerThreshold;
    //贝塞尔离场动画
    private ValueAnimator besselOutAnimator;
    //贝塞尔动画时间
    private int besselOutDuration = 500;
    //贝塞尔动画速度
    private int besselOutSpeed = dp2px(5);
    //贝塞尔颜色
    private int besselColor;
    //抽屉动画
    private ValueAnimator drawerAnimator;
    //抽屉尺寸百分比
    private float drawerPercent = 0.7f;
    //抽屉动画时间
    private int drawerDuration = 500;
    //抽屉进场插值器
    private DecelerateInterpolator openInterpolator = new DecelerateInterpolator();
    //抽屉进场插值器
    private DecelerateInterpolator closeInterpolator = new DecelerateInterpolator();
    //抽屉位移量
    private int amount = 0;
    //遮罩
    private Rect maskRect = new Rect();
    //遮罩画笔
    private Paint maskPaint = new Paint();
    //遮罩透明度
    private float maskAlphaPercent = 0.7f;
    //贝塞尔颜色是否依附抽屉背景色
    private boolean attachDrawerColor;


    private OnSimpleDrawerLayoutCallBack onSimpleDrawerLayoutCallBack;

    public void setOnSimpleDrawerLayoutCallBack(OnSimpleDrawerLayoutCallBack onSimpleDrawerLayoutCallBack) {
        this.onSimpleDrawerLayoutCallBack = onSimpleDrawerLayoutCallBack;
    }

    public void clear() {
        if (besselOutAnimator != null) {
            besselOutAnimator.removeUpdateListener(animatorUpdateListener);
            besselOutAnimator.removeListener(animatorListenerAdapter);
        }
        besselOutAnimator = null;
        if (drawerAnimator != null) {
            drawerAnimator.removeUpdateListener(animatorUpdateListener);
            drawerAnimator.removeListener(animatorListenerAdapter);
        }
        drawerAnimator = null;
    }

    private ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (animation == besselOutAnimator) {
                besselCoordinates(besselX, besselY);
                invalidate();
            }
            if (animation == drawerAnimator) {
                amount = (int) animation.getAnimatedValue();
                drawerLocation();
                maskColor();
                getChildAt(1).setAlpha(getAlphaPercent());
                if (onSimpleDrawerLayoutCallBack != null) {
                    onSimpleDrawerLayoutCallBack.onDrawerStatusChanged(SimpleDrawerLayout.this, drawerStatus, amount, MOVING);
                }
            }
        }
    };

    private AnimatorListenerAdapter animatorListenerAdapter = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            if (animation == drawerAnimator) {
                if (drawerAnimator.getInterpolator() == openInterpolator) {
                    drawerStatus = OPENING;
                } else if (drawerAnimator.getInterpolator() == closeInterpolator) {
                    drawerStatus = CLOSING;
                }
                if (onSimpleDrawerLayoutCallBack != null) {
                    onSimpleDrawerLayoutCallBack.onDrawerStatusChanged(SimpleDrawerLayout.this, drawerStatus, amount, START);
                }
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if (animation == besselOutAnimator) {
                reset();
            }
            if (animation == drawerAnimator) {
                if (drawerAnimator.getInterpolator() == openInterpolator) {
                    drawerStatus = OPENED;
                    maskPaint.setAlpha((int) (255 * maskAlphaPercent));
                    invalidate();
                } else if (drawerAnimator.getInterpolator() == closeInterpolator) {
                    drawerStatus = CLOSED;
                    maskPaint.setAlpha(0);
                    invalidate();
                }
                if (onSimpleDrawerLayoutCallBack != null) {
                    onSimpleDrawerLayoutCallBack.onDrawerStatusChanged(SimpleDrawerLayout.this, drawerStatus, amount, END);
                }
            }
        }
    };

    public SimpleDrawerLayout(Context context) {
        super(context);
        init();
    }

    public SimpleDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        besselPaint.setAntiAlias(true);
        maskPaint.setAntiAlias(true);
        post(new Runnable() {
            @Override
            public void run() {
                if (getChildAt(1) != null) {
                    initDrawerSize();
                    besselBackgroundColor(getChildAt(1).getBackground());
                    besselPaint.setColor(attachDrawerColor ? besselColor : BACKGROUND_COLOR);
                    getChildAt(1).setOnTouchListener(new OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            //TODO 这里不做什么操作，仅仅拦截抽屉的触摸事件不让其穿透
                            return true;
                        }
                    });
                }
            }
        });
    }

    /**
     * 初始化抽屉尺寸
     */
    private void initDrawerSize() {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        switch (gravity) {
            case Gravity.RIGHT:
            case Gravity.END:
            case Gravity.LEFT:
            case Gravity.START:
                layoutParams.width = (int) (getWidth() * drawerPercent);
                layoutParams.height = getHeight();
                break;
            case Gravity.TOP:
            case Gravity.BOTTOM:
                layoutParams.width = getWidth();
                layoutParams.height = (int) (getHeight() * drawerPercent);
                break;
        }
        getChildAt(1).setLayoutParams(layoutParams);
    }

    /**
     * 初始化贝塞尔背景色
     */
    private void besselBackgroundColor(Drawable drawable) {
        if (drawable == null) {
            besselColor = BACKGROUND_COLOR;
            return;
        }
        besselColor = drawable instanceof ColorDrawable ? ((ColorDrawable) drawable).getColor() : BACKGROUND_COLOR;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() != 2) {
            throw new RuntimeException("子布局数量错误！请在xml中放入两个子布局，第一个为主布局，第二个为抽屉布局。");
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        getChildAt(0).layout(0, 0, getChildAt(0).getMeasuredWidth(), getChildAt(0).getMeasuredHeight());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawPath(besselPath, besselPaint);
        canvas.drawRect(maskRect, maskPaint);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return isContactPointEffective(ev) && drawerStatus == CLOSED || drawerStatus == CLOSING || drawerStatus == OPENING || drawerStatus == OPENED && isContactPointMaskRect(ev);
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (drawerStatus == OPENING || drawerStatus == CLOSING) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = (int) event.getX();
                y = (int) event.getY();
                if (trigger(event, false) && drawerStatus == OPENED) {
                    closeDrawers();
                } else {
                    if (drawerStatus != OPENED && isContactPointEffective(event)) {
                        status = PREPARE;
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (outOfRect(event) && drawerStatus != OPENED) {
                    status = OUT;
                    besselOutAnimation();
                    return true;
                }
                if (status == PREPARE) {
                    status = DRAG;
                }
                if (event.getX() - x < besselThreshold && status == DRAG) {
                    besselCoordinates((int) event.getX(), (int) event.getY());
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (!outOfRect(event) && status == DRAG) {
                    if (drawerStatus == CLOSED) {
                        status = OUT;
                        besselOutAnimation();
                    }
                    if (trigger(event, true)) {
                        openDrawer();
                    }
                    if (drawerStatus == OPENED && !isContactPointMaskRect(event)) {
                        getChildAt(1).invalidate();
                    }
                }
                break;
        }
        return super.onTouchEvent(event);

    }

    /**
     * 计算抽屉位置
     */
    private void drawerLocation() {
        switch (gravity) {
            case Gravity.RIGHT:
            case Gravity.END:
                getChildAt(1).layout(getWidth() - amount, 0, getWidth() - amount + getDrawerSize(), getChildAt(1).getMeasuredHeight());
                break;
            case Gravity.LEFT:
            case Gravity.START:
                getChildAt(1).layout(-getDrawerSize() + amount, 0, amount, getChildAt(1).getMeasuredHeight());
                break;
            case Gravity.TOP:
                getChildAt(1).layout(0, -getDrawerSize() + amount, getWidth(), amount);
                break;
            case Gravity.BOTTOM:
                getChildAt(1).layout(0, getHeight() - amount, getWidth(), getHeight() + getDrawerSize() - amount);
                break;
        }
    }

    /**
     * 计算遮罩范围及透明度
     */
    private void maskColor() {
        switch (gravity) {
            case Gravity.RIGHT:
            case Gravity.END:
                maskRect.left = 0;
                maskRect.top = 0;
                maskRect.right = getWidth() - amount;
                maskRect.bottom = getHeight();
                break;
            case Gravity.LEFT:
            case Gravity.START:
                maskRect.left = amount;
                maskRect.top = 0;
                maskRect.right = getWidth();
                maskRect.bottom = getHeight();
                break;
            case Gravity.TOP:
                maskRect.left = 0;
                maskRect.top = amount;
                maskRect.right = getWidth();
                maskRect.bottom = getHeight();
                break;
            case Gravity.BOTTOM:
                maskRect.left = 0;
                maskRect.top = 0;
                maskRect.right = getWidth();
                maskRect.bottom = getHeight() - amount;
                break;
        }
        maskPaint.setAlpha((int) (getAlphaPercent() * 255 * maskAlphaPercent));
        invalidate();
    }

    /**
     * 计算遮罩范围及透明度
     */
    private boolean isContactPointMaskRect(MotionEvent event) {
        return event.getX() > maskRect.left && event.getX() < maskRect.right && event.getY() > maskRect.top && event.getY() < maskRect.bottom;
    }

    private DecimalFormat decimalFormat = new DecimalFormat("0.000");

    /**
     * 计算抽屉透明度
     */
    private float getAlphaPercent() {
        switch (gravity) {
            case Gravity.LEFT:
            case Gravity.START:
            case Gravity.RIGHT:
            case Gravity.END:
                return Float.parseFloat(decimalFormat.format(amount * 1.0 / getChildAt(1).getWidth()));
            case Gravity.TOP:
            case Gravity.BOTTOM:
                return Float.parseFloat(decimalFormat.format(amount * 1.0 / getChildAt(1).getHeight()));
        }
        return 255f;
    }

    /**
     * 计算触点是否越界
     */
    private boolean outOfRect(MotionEvent event) {
        if (event.getX() > getWidth() || event.getX() < 0 || event.getY() > getHeight() || event.getY() < 0) {
            return true;
        }
        return false;
    }

    /**
     * 计算触点是否位于屏幕四周（贝塞尔绘制区域）
     */
    private boolean isContactPointEffective(MotionEvent event) {
        switch (gravity) {
            case Gravity.LEFT:
            case Gravity.START:
                return event.getX() < triggerThreshold;
            case Gravity.RIGHT:
            case Gravity.END:
                return event.getX() > getWidth() - triggerThreshold;
            case Gravity.TOP:
                return event.getY() < triggerThreshold;
            case Gravity.BOTTOM:
                return event.getY() > getHeight() - triggerThreshold;
        }
        return false;
    }

    /**
     * 计算贝塞尔坐标
     */
    private void besselCoordinates(int eventX, int eventY) {
        besselPath.reset();
        switch (gravity) {
            case Gravity.LEFT:
            case Gravity.START:
                besselPath.moveTo(0, 0);
                if (status == OUT) {
                    besselPath.cubicTo(0, 0, besselX -= besselOutSpeed, besselY, 0, getHeight());
                } else {
                    if (Math.abs(x - eventX) > besselThreshold) {
                        besselX = besselThreshold;
                        besselY = eventY;
                        besselPath.cubicTo(0, 0, besselX, besselY, 0, getHeight());
                    } else {
                        besselX = Math.abs(x - eventX);
                        besselY = eventY;
                        besselPath.cubicTo(0, 0, besselX, besselY, 0, getHeight());
                    }
                }
                break;
            case Gravity.RIGHT:
            case Gravity.END:
                besselPath.moveTo(getWidth(), 0);
                if (status == OUT) {
                    besselPath.cubicTo(getWidth(), 0, besselX += besselOutSpeed, besselY, getWidth(), getHeight());
                } else {
                    if (Math.abs(eventX - x) > besselThreshold) {
                        besselX = getWidth() - besselThreshold;
                        besselY = eventY;
                        besselPath.cubicTo(getWidth(), 0, besselX, besselY, getWidth(), getHeight());
                    } else {
                        besselX = getWidth() - Math.abs(eventX - x);
                        besselY = eventY;
                        besselPath.cubicTo(getWidth(), 0, besselX, besselY, getWidth(), getHeight());
                    }
                }
                break;
            case Gravity.TOP:
                besselPath.moveTo(0, 0);
                if (status == OUT) {
                    besselPath.cubicTo(0, 0, besselX, besselY -= besselOutSpeed, getWidth(), 0);
                } else {
                    if (Math.abs(y - eventY) > besselThreshold) {
                        besselX = eventX;
                        besselY = besselThreshold;
                        besselPath.cubicTo(0, 0, besselX, besselY, getWidth(), 0);
                    } else {
                        besselX = eventX;
                        besselY = Math.abs(y - eventY);
                        besselPath.cubicTo(0, 0, besselX, besselY, getWidth(), 0);
                    }
                }
                break;
            case Gravity.BOTTOM:
                besselPath.moveTo(0, getHeight());
                if (status == OUT) {
                    besselPath.cubicTo(0, getHeight(), besselX, besselY += besselOutSpeed, getWidth(), getHeight());
                } else {

                    if (Math.abs(y - eventY) > besselThreshold) {
                        besselX = eventX;
                        besselY = getHeight() - besselThreshold;
                        besselPath.cubicTo(0, getHeight(), besselX, besselY, getWidth(), getHeight());
                    } else {
                        besselX = eventX;
                        besselY = getHeight() - Math.abs(y - eventY);
                        besselPath.cubicTo(0, getHeight(), besselX, besselY, getWidth(), getHeight());
                    }
                }
                break;
        }
        besselPath.close();
    }

    /**
     * 计算触点是否达到开启/关闭抽屉的程度
     */
    private boolean trigger(MotionEvent event, boolean isOpen) {
        switch (gravity) {
            case Gravity.LEFT:
            case Gravity.START:
                return isOpen ? event.getX() > triggerThreshold : event.getX() > getDrawerSize();
            case Gravity.RIGHT:
            case Gravity.END:
                return isOpen ? event.getX() < getWidth() - triggerThreshold : event.getX() < getWidth() - getDrawerSize();
            case Gravity.TOP:
                return isOpen ? event.getY() > triggerThreshold : event.getY() > getDrawerSize();
            case Gravity.BOTTOM:
                return isOpen ? event.getY() < getHeight() - triggerThreshold : event.getY() < getHeight() - getDrawerSize();
        }
        return false;
    }

    /**
     * 按百分比获取侧滑抽屉宽高尺寸
     */
    private int getDrawerSize() {
        switch (gravity) {
            case Gravity.RIGHT:
            case Gravity.END:
            case Gravity.LEFT:
            case Gravity.START:
                return (int) (getWidth() * drawerPercent);
            case Gravity.TOP:
            case Gravity.BOTTOM:
                return (int) (getHeight() * drawerPercent);
        }
        return 0;
    }

    /**
     * 贝塞尔出场动画
     */
    private void besselOutAnimation() {
        if (besselOutAnimator == null) {
            besselOutAnimator = ValueAnimator.ofInt(0, 100);
            besselOutAnimator.addUpdateListener(animatorUpdateListener);
            besselOutAnimator.addListener(animatorListenerAdapter);
            besselOutAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            besselOutAnimator.setDuration(besselOutDuration);
        }
        besselOutAnimator.start();
    }

    /**
     * 设置抽屉方向
     */
    public void setGravity(int gravity) {
        if (gravity == Gravity.LEFT ||
                gravity == Gravity.START ||
                gravity == Gravity.RIGHT ||
                gravity == Gravity.END ||
                gravity == Gravity.TOP ||
                gravity == Gravity.BOTTOM) {
            this.gravity = gravity;
        }
    }

    /**
     * 获取抽屉方向
     */
    public int getGravity() {
        return gravity;
    }

    /**
     * 打开抽屉（内部调用）
     */
    private void openDrawer() {
        createDrawerAnimator();
        drawerAnimator.setRepeatMode(ValueAnimator.RESTART);
        drawerAnimator.setIntValues(0, getDrawerSize());
        drawerAnimator.setInterpolator(openInterpolator);
        drawerAnimator.start();
    }

    /**
     * 打开抽屉
     */
    public void openDrawer(int gravity) {
        if (drawerStatus != OPENED) {
            setGravity(gravity);
            openDrawer();
        }
    }

    /**
     * 关闭抽屉
     */
    public void closeDrawers() {
        if (drawerStatus != CLOSED) {
            createDrawerAnimator();
            drawerAnimator.setRepeatMode(ValueAnimator.REVERSE);
            drawerAnimator.setIntValues(getDrawerSize(), 0);
            drawerAnimator.setInterpolator(closeInterpolator);
            drawerAnimator.start();
        }
    }

    /**
     * 创建抽屉动画
     */
    private void createDrawerAnimator() {
        if (drawerAnimator == null) {
            drawerAnimator = ValueAnimator.ofInt(0, getDrawerSize());
            drawerAnimator.addUpdateListener(animatorUpdateListener);
            drawerAnimator.addListener(animatorListenerAdapter);
            drawerAnimator.setDuration(drawerDuration);
        }
    }

    /**
     * dp转px
     */
    public int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * 设置抽屉尺寸（0f-1f）
     */
    public void setDrawerPercent(float drawerPercent) {
        this.drawerPercent = drawerPercent;
        openDrawer();
    }

    /**
     * 获取抽屉的状态
     */
    public int getDrawerStatus() {
        return drawerStatus;
    }

    /**
     * 贝塞尔颜色是否依附抽屉背景色
     */
    public void setAttachDrawerColor(boolean attachDrawerColor) {
        this.attachDrawerColor = attachDrawerColor;
        besselPaint.setColor(this.attachDrawerColor ? besselColor : BACKGROUND_COLOR);
    }

    /**
     * 设置抽屉背景色
     */
    public void setBackgroundColor(@ColorInt int color) {
        if (getChildCount() == 2) {
            getChildAt(1).setBackgroundColor(color);
            besselBackgroundColor(getChildAt(1).getBackground());
            if (attachDrawerColor) {
                besselPaint.setColor(besselColor);
            }
        }
    }

    /**
     * 设置贝塞尔颜色
     */
    public void setBesselColor(int alpha, int red, int green, int blue) {
        if (attachDrawerColor) {
            return;
        }
        this.besselColor = (alpha > -1 && red > -1 && green > -1 && blue > -1) ? Color.argb(alpha, red, green, blue) : BACKGROUND_COLOR;
        besselPaint.setColor(this.besselColor);
    }

    /**
     * 重置
     */
    private void reset() {
        besselPath.reset();
        status = NONE;
        x = 0;
        y = 0;
        besselX = 0;
        besselY = 0;
    }

    public interface OnSimpleDrawerLayoutCallBack {

        void onDrawerStatusChanged(SimpleDrawerLayout drawerLayout, int status, int amount, int state);
    }

}
