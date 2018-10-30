package lt.example.com.myrefreshview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.OverScroller;
import android.widget.TextView;

public class MyRefreshViewListView extends FrameLayout {
    private ListView contentView;
    private View headerView;
    private View footerView;
    private Context context;
    private OverScroller mScroller;
    private int refresh_height;//下拉刷新时候固定显示高度
    private int load_height;//上拉加载时候固定显示高度
    private TextView refresh_txt,load_txt;//下拉、上拉文字提示
    private boolean isTop;//true 下拉，false 上拉
    private boolean isRefreshOrLoad;//是否正在刷新或者加载
    private View btm_refresh_pro;//上拉刷新圆圈
    private View top_refresh_img;//上拉刷新图标
    private View top_refresh_pro;//下拉刷新圆圈
    private RotateAnimation upRotateAnima,downRotateAnima;
    private boolean reset;//刷新控件滑动隐藏时滑动过快调用过多控制位
    private RefreshListerner refreshListerner;//监听事件
    private float lasty=0;//上次保存的y值
    int realSubY;//实际移动的高度
    int subY;//每次滑动的y值

    public interface RefreshListerner{
        void onRefresh();
        void onLoad();
        void onCancel();
    }

    public MyRefreshViewListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        mScroller = new OverScroller(this.context);
    }

    public void setRefreshListerner(RefreshListerner refreshListerner) {
        this.refreshListerner = refreshListerner;
    }

    private void initView(){
        contentView = (ListView) getChildAt(0);
        LayoutInflater.from(getContext()).inflate(R.layout.refresh_header_layout,this,true);
        headerView = getChildAt(getChildCount()-1);
        refresh_txt = headerView.findViewById(R.id.top_refresh_txt);
        top_refresh_img = headerView.findViewById(R.id.top_refresh_img);
        top_refresh_pro = headerView.findViewById(R.id.top_refresh_pro);
        LayoutInflater.from(getContext()).inflate(R.layout.refresh_footer_layout,this,true);
        footerView = getChildAt(getChildCount()-1);
        load_txt = footerView.findViewById(R.id.btm_refresh_txt);
        btm_refresh_pro = footerView.findViewById(R.id.btm_refresh_pro);
        upRotateAnima = new RotateAnimation(-180,0,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        upRotateAnima.setFillAfter(true);//动画执行完后是否停留在执行完的状态
        upRotateAnima.setDuration(200);
        downRotateAnima = new RotateAnimation(0,-180,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        downRotateAnima.setFillAfter(true);//动画执行完后是否停留在执行完的状态
        downRotateAnima.setDuration(200);

    }

    @Override
    protected void onFinishInflate() {
        initView();
        super.onFinishInflate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(getChildCount()>0){
            for (int i=0;i<getChildCount();i++){
                View childView = getChildAt(i);
                measureChild(childView,widthMeasureSpec,heightMeasureSpec);
            }
        }
        if(headerView!=null){
            refresh_height = headerView.getMeasuredHeight();
        }
        if(footerView!=null){
            load_height = footerView.getMeasuredHeight();
        }
        setMeasuredDimension(widthMeasureSpec,heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(headerView!=null)
            headerView.layout(0,-refresh_height,getWidth(),0);
        if(footerView!=null)
            footerView.layout(0,getHeight(),getWidth(),getHeight()+load_height);
        if(contentView!=null)
            contentView.layout(0,0,contentView.getMeasuredWidth(),contentView.getMeasuredHeight());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lasty = ev.getY();
                if (isRefreshOrLoad){//正在刷新或者加载时候直接中断
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                subY = (int) (ev.getY() - lasty);
                if(subY<0 && !contentView.canScrollVertically(1)){//上拉加载判断
                    isTop = false;
                    return true;//拦截触摸事件
                }
                if(subY>0 && !contentView.canScrollVertically(-1)){//下拉刷新判断
                    isTop = true;
                    return true;//拦截触摸事件
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:
//                getParent().requestDisallowInterceptTouchEvent(true);
                float mY = event.getY();
                subY = (int) (mY - lasty);
                if(isRefreshOrLoad){//正在刷新或者加载时候直接返回true
                    return true;
                }
                if (isTop) {
                    if((realSubY+subY)>refresh_height){//下拉高度大于控件高度时候减缓高度变化
                        subY = (int) ((float) ((refresh_height * 1.2 + getScrollY()) / (float) refresh_height) * subY);
                    }else if((realSubY+subY)<0){//完全隐藏下拉刷新控件时
                        if(!reset && getScrollY()<0){//滑动过快，响应不及时处理
                            subY = getScrollY();
                            reset=true;
                        }else {
                            event.setAction(MotionEvent.ACTION_DOWN);
                            reset=false;
                            return dispatchTouchEvent(event);//分发到下一层控件
                        }
                    }
                }else {
                    if((-(subY+realSubY))>load_height){//上拉高度大于控件高度时候减缓高度变化
                        subY = (int) ((float) ((load_height*1.2 - getScrollY()) / (float) load_height) * subY);
                    }else if(-(subY+realSubY)<0){//完全隐藏上拉刷新控件时
                        if(!reset && getScrollY()>0){//滑动过快，响应不及时处理
                            subY = getScrollY();
                            reset=true;
                        }else {
                            if(getScrollY()>0){
                                scrollBy(0, -getScrollY());
                            }
                            event.setAction(MotionEvent.ACTION_DOWN);
                            dispatchTouchEvent(event);
                            return super.onTouchEvent(event);
                        }
                    }
                }
                scrollBy(0, -subY);
                lasty = mY;
                realSubY = - getScrollY();

                if(!isRefreshOrLoad) {//未执行刷新或者加载时，控件内容随滑动高度变化
                    if (realSubY > 0 && realSubY < refresh_height) {
                        refresh_txt.setText("下拉刷新");
                        if (top_refresh_img.getAnimation() == downRotateAnima) {
                            top_refresh_img.startAnimation(upRotateAnima);
                        }
                    } else if (realSubY >= refresh_height) {
                        if (top_refresh_img.getAnimation() != downRotateAnima)
                            top_refresh_img.startAnimation(downRotateAnima);
                        refresh_txt.setText("松开刷新");
                    } else if (realSubY <= (-load_height)) {
                        load_txt.setText("松开加载更多");
                    } else if (realSubY < 0 && realSubY > (-load_height)) {
                        load_txt.setText("上拉加载更多");
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                reset=false;
                if(isTop){
                    if(isRefreshOrLoad && subY<60){//判断刷新时候上滑取消
                        if(refreshListerner!=null)
                            refreshListerner.onCancel();
                        onFinishRereshOrLoad();
                        break;
                    }
                    if(realSubY>=0 && realSubY<refresh_height) {//滑动结束判断刷新情况
                        onFinishRereshOrLoad();
                    }else if(realSubY>=refresh_height){
                        mScroller.startScroll(0, getScrollY(), 0, realSubY-refresh_height, 300);
                        realSubY=refresh_height;
                        refresh_txt.setText("正在刷新...");
                        top_refresh_img.setVisibility(GONE);
                        top_refresh_img.clearAnimation();//不加clearAnimation Gone后依然会显示top_refresh_img
                        top_refresh_pro.setVisibility(VISIBLE);
                        isRefreshOrLoad=true;
                        if(refreshListerner!=null)
                            refreshListerner.onRefresh();
                    }
                }else {
                    if(isRefreshOrLoad && subY>60){//判断加载时候下滑取消
                        if(refreshListerner!=null)
                            refreshListerner.onCancel();
                        onFinishRereshOrLoad();
                        break;
                    }
                    if(realSubY<0 && realSubY>(-load_height)){//滑动结束判断加载情况
                        onFinishRereshOrLoad();
                    }else if(realSubY<=(-load_height)){
                        mScroller.startScroll(0, getScrollY(), 0, load_height-getScrollY(), 300);
                        realSubY=-load_height;
                        btm_refresh_pro.setVisibility(VISIBLE);
                        load_txt.setText("正在加载...");
                        isRefreshOrLoad=true;
                        if(refreshListerner!=null)
                            refreshListerner.onLoad();
                    }
                }
                invalidate();
                break;
        }
        return true;
    }
    @Override
    public void computeScroll() {
        if(mScroller.computeScrollOffset()){
            scrollTo(0,mScroller.getCurrY());
            invalidate();
        }
    }

    /**
     * 结束刷新或者加载
     */
    public void onFinishRereshOrLoad(){
        mScroller.startScroll(0, getScrollY(), 0, -getScrollY(), 300);
        realSubY=0;
        isRefreshOrLoad=false;
        if(isTop){
            finishRefresh();
        }else {
            finishload();
        }
    }

    /**
     * 结束刷新
     */
    private void finishRefresh(){
        top_layout_value1();
    }
    /**
     * 结束加载
     */
    private void finishload(){
        btm_layout_value1();
    }

    private void top_layout_value1(){
        refresh_txt.setText("下拉刷新");
        top_refresh_pro.setVisibility(GONE);
        top_refresh_img.setVisibility(VISIBLE);
        top_refresh_img.clearAnimation();
    }
    private void btm_layout_value1(){
        btm_refresh_pro.setVisibility(GONE);
        load_txt.setText("上拉加载");
    }
}
