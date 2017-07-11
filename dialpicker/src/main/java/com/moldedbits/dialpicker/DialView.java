package com.moldedbits.dialpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;

import timber.log.Timber;

import static java.lang.Math.PI;

/**
 * Created by anilmanchanda
 * on 3/8/17.
 */

public class DialView extends View {

    private static final int CENTER_OFFSET = 40;
    private static final int CENTER_OFFSET_VERTICAL = 40;
    private static final float VELOCITY_THRESHOLD = 0.05f;
    Logger logger = Logger.getLogger("DialView");
    /**
     * User is not touching the list
     */
    private static final int TOUCH_STATE_RESTING = 0;

    /**
     * User is touching the list and right now it's still a "click"
     */
    private static final int TOUCH_STATE_CLICK = 1;

    /**
     * User is scrolling the list
     */
    private static final int TOUCH_STATE_SCROLL = 2;

    /**
     * Velocity tracker used to get fling velocities
     */
    private VelocityTracker velocityTracker;

    /**
     * Current touch state
     */
    private int touchState = TOUCH_STATE_RESTING;

    private static final int RADIANS_PER_SECOND = 1;

    private double tickCount;
    private double currentTheta;
    private double initTheta;
    private Rect bounds = new Rect();
    private double minAngleTheta;
    private double maxAngleTheta;
    private double tickGapAngle;
    private long currentTime;
    private int maxValue;
    private int minValue;
    private int lineInterval;
    private int textSize;
    private int leastCount;
    private int centerPadding;
    private int dialDirection;
    private int angleToCompare;
    private double delta;
    private int startColor;
    private int endColor;
    private int paintTextColor;
    private int paintLineColor;
    private int paintArcColor;

    /**
     * Knob deceleration
     */
    private float deceleration = 10f;

    private int centerX;
    private int centerY;
    private int radius;
    private Paint paintInnerCircle;
    private Paint paintArc;
    private Paint paintLines;
    private Paint paintText;

    private OnDialValueChangeListener onDialValueChangeListener;

    public void setOnDialValueChangeListener(OnDialValueChangeListener listener) {
        this.onDialValueChangeListener = listener;
    }

    public DialView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public DialView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, context);
    }

    private float initVelocity = 0.5f;

    /**
     * Physics implementation
     */

    Runnable dynamicsRunnable = new Runnable() {
        @Override
        public void run() {
            if (Math.abs(initVelocity) < VELOCITY_THRESHOLD) {
                return;
            }
            long newTime = System.nanoTime();
            long deltaNano = (newTime - currentTime);
            double deltaSecs = ((double) deltaNano) / 1000000000;
            currentTime = newTime;
            float finalVelocity;
            if (initVelocity > 0) {
                finalVelocity = (float) (initVelocity - deceleration * deltaSecs);
            } else {
                finalVelocity = (float) (initVelocity + deceleration * deltaSecs);
            }
            if (initVelocity * finalVelocity < 0) {
                return;
            }
            rotate(finalVelocity * deltaSecs);
            invalidate();
            DialView.this.postDelayed(dynamicsRunnable, 1000 / 60);
            initVelocity = finalVelocity;
        }
    };

    /**
     * @param attrs   are the attributes containing the values given by user
     * @param context context of the activity to use this view class
     */

    private void init(AttributeSet attrs, Context context) {
        paintInnerCircle = new Paint();
        paintArc = new Paint();
        paintLines = new Paint();
        paintText = new Paint();

        paintArc.setStyle(Paint.Style.STROKE);
        paintArc.setPathEffect(new DashPathEffect(new float[]{5, 10}, 0));
        paintLines.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.RIGHT);


        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DialView);
            lineInterval = typedArray.getInt(R.styleable.DialView_lineInterval, 0);
            maxValue = typedArray.getInt(R.styleable.DialView_maxValue, 0);
            minValue = typedArray.getInt(R.styleable.DialView_minValue, 0);
            leastCount = typedArray.getInt(R.styleable.DialView_leastCount, 0);
            centerPadding = typedArray.getInt(R.styleable.DialView_centerPadding, 0);
            textSize = typedArray.getInt(R.styleable.DialView_textSize, 0);
            dialDirection = typedArray.getInt(R.styleable.DialView_dialDirection, 0);
            tickGapAngle = ((double) typedArray.getInt(R.styleable.DialView_tickGapAngle, 0)
                    / (double) 180) * PI;
            startColor = typedArray.getColor(R.styleable.DialView_startColor, 0);
            endColor = typedArray.getColor(R.styleable.DialView_endColor, 0);
            paintLineColor=typedArray.getColor(R.styleable.DialView_paintLineColor,0);
            paintTextColor=typedArray.getColor(R.styleable.DialView_paintTextColor,0);
            paintArcColor=typedArray.getColor(R.styleable.DialView_paintArcColor,0);
            typedArray.recycle();
        }

        if (minValue >= maxValue) {
            maxValue = minValue;
            minValue = 0;
        }

//        leastCount = (leastCount != 1) ? 1 : leastCount;


        paintInnerCircle.setStyle(Paint.Style.FILL);
        paintInnerCircle.setFilterBitmap(true);
        paintInnerCircle.setShader(new LinearGradient(0, 0, 0, getHeight(), endColor, startColor, Shader.TileMode.CLAMP));
        paintLines.setColor(paintLineColor);
        paintText.setColor(paintTextColor);
        paintArc.setColor(paintArcColor);

        switch (dialDirection) {
            //for left
            case 1:
                currentTheta = 0;
                initTheta = 0;
                angleToCompare = 0;
                break;
            //for top
            case 2:
                currentTheta = PI / 2;
                initTheta = PI / 2;
                angleToCompare = 90;
                break;
            //for right
            case 3:
                currentTheta = PI;
                initTheta = PI;
                angleToCompare = 180;
                break;
            //for bottom
            case 4:
                currentTheta = PI * 3 / 2;
                initTheta = PI * 3 / 2;
                angleToCompare = 270;
                break;

            default:
                // // TODO: 4/5/17 nothing
                break;
        }
        paintText.setTextSize(textSize);
    }

    /**
     * @param widthMeasureSpec  is the width of canvas
     * @param heightMeasureSpec is the height of canvas
     *                          used to calculate all the radius and centerX and centerY
     *                          MaxAngleTheta calculated
     */

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Timber.d("tick gap angle is %f", tickGapAngle);
        tickCount = ((maxValue - minValue) / leastCount) + 1;
        maxAngleTheta = (((tickCount - 1) * tickGapAngle));
        minAngleTheta = 0;

        switch (dialDirection) {
            // for left
            case 1:
                centerX = 0 - CENTER_OFFSET;
                radius = getMeasuredHeight() / 2 - centerPadding;
                centerY = getMeasuredHeight() / 2;
                break;
            // for top
            case 2:
                centerX = getMeasuredWidth() / 2;
                radius = getMeasuredWidth() / 2 - centerPadding;
                centerY = 0 - CENTER_OFFSET_VERTICAL;
                break;
            //for right
            case 3:
                centerX = getMeasuredWidth() + CENTER_OFFSET;
                radius = getMeasuredHeight() / 2 - centerPadding;
                centerY = getMeasuredHeight() / 2;
                break;
            //for bottom
            case 4:
                centerX = getMeasuredWidth() / 2;
                radius = getMeasuredWidth() / 2 - centerPadding;
                centerY = getMeasuredHeight() + CENTER_OFFSET_VERTICAL;
                break;

            default:
                // // TODO: 4/5/17 nothing
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        makeRadGrad(canvas);
    }

    /**
     * @param canvas to draw the circle and lines
     */

    private void makeRadGrad(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, radius, paintInnerCircle);
        canvas.drawCircle(centerX, centerY, radius, paintArc);

        for (double i = minValue; i < tickCount; i++) {
            double angle = i * tickGapAngle;

            int lineHeight;
            if (i % lineInterval == 0) {
                lineHeight = 30;
            } else {
                lineHeight = 20;
            }

            double newTheta = 0;
            if (angleToCompare == 0) {
                //for left
                newTheta = currentTheta - angle;
            } else if (angleToCompare == 90) {
                //for top
                newTheta = angle + currentTheta;
            } else if (angleToCompare == 180) {
                //for right
                newTheta = angle + currentTheta;
            } else if (angleToCompare == 270) {
                //for bottom
                newTheta = angle + currentTheta;
            }

            float startX = (float) (((radius + 10) * Math.cos(newTheta)) + centerX);
            float startY = (float) (((radius + 10) * Math.sin(newTheta)) + centerY);

            float endX = (float) (((radius + lineHeight) * Math.cos(newTheta)) + centerX);
            float endY = (float) (((radius + lineHeight) * Math.sin(newTheta)) + centerY);

            float textPointX = (float) (((radius + lineHeight + 10) * Math.cos(newTheta)) + centerX);
            float textPointY = (float) (((radius + lineHeight + 10) * Math.sin(newTheta)) + centerY);

            if (lineHeight == 30) {
                addingTextValuesToDial(canvas, newTheta, (int) i, textPointX, textPointY);
            }

            canvas.drawLine(startX, startY, endX, endY, paintLines);

            int newThetaInDegree = (int) (newTheta / PI * 180);
            NumberFormat formatter = new DecimalFormat("00");
            String value = formatter.format(i);
            switch (dialDirection) {
                //for left
                case 1:
                    if (newThetaInDegree == angleToCompare) {
                        if (onDialValueChangeListener != null) {
                            onDialValueChangeListener.onDialValueChanged(value, maxValue);
                        }
                    }
                    break;
                //for top
                case 2:
                    if (newThetaInDegree == angleToCompare
                            || newThetaInDegree < (angleToCompare + 0.2)) {
                        if (onDialValueChangeListener != null) {
                            onDialValueChangeListener.onDialValueChanged(value, maxValue);
                        }
                    }
                    break;
                //for right
                case 3:
                    if (newThetaInDegree == angleToCompare
                            || newThetaInDegree < (angleToCompare + 0.2)) {
                        if (onDialValueChangeListener != null) {
                            onDialValueChangeListener.onDialValueChanged(value, maxValue);
                        }
                    }
                    break;
                //for bottom
                case 4:
                    if (newThetaInDegree == angleToCompare) {
                        if (onDialValueChangeListener != null) {
                            onDialValueChangeListener.onDialValueChanged(value, maxValue);
                        }
                    }
                    break;

                default:
                    // // TODO: 4/5/17 nothing
                    break;
            }
        }
    }

    /**
     * @param canvas is the canvas on which the text will be drawn
     * @param angle  is the angle at which the text is to be drawn
     * @param value  is the difference from minimum value to max value of dial
     *               divided by line interval
     * @param startX starting x from where the text will start
     * @param startY starting Y from where the text will start
     */

    private void addingTextValuesToDial(Canvas canvas, double angle,
                                        int value, float startX, float startY) {
        double newAngle = angle / PI * 180;
        int angleInteger = (int) newAngle;
        paintText.getTextBounds(String.valueOf(value), 0, 1, bounds);

        // to be extracted afterwards when initialised by user
        if (angleToCompare == 0) {
            // for left
            paintText.setTextAlign(Paint.Align.LEFT);
            if (angleInteger <= angleToCompare) {
                canvas.drawText(String.valueOf(value), startX, startY + bounds.height() * 2 / 3, paintText);
            } else {
                canvas.drawText(String.valueOf(value), startX, startY + bounds.height() / 2, paintText);
            }
        } else if (angleToCompare == 90) {
            //for top
            if (angleInteger >= angleToCompare && angleInteger < 360) {
                canvas.drawText(String.valueOf(value), startX + 8 * 2 / 3, startY + 11 * 2 / 3, paintText);
            } else {
                canvas.drawText(String.valueOf(value), (float) (startX + 8 * 0.9), startY + 11 * 2 / 3, paintText);
            }
        } else if (angleToCompare == 180) {
            // for right
            if (angleInteger >= angleToCompare - (int) (lineInterval * (tickGapAngle * 180 / PI))) {
                canvas.drawText(String.valueOf(value), startX, startY + bounds.height() / 2, paintText);
            } else if (angleInteger < 180 - (int) (lineInterval * (tickGapAngle * 180 / PI))) {
                canvas.drawText(String.valueOf(value), startX, startY + bounds.height() * 2 / 3, paintText);
            }
        } else if (angleToCompare == 270) {
            // for bottom
            if (angleInteger >= angleToCompare && angleInteger < 360) {
                canvas.drawText(String.valueOf(value), startX + 8 * 3 / 4, startY - 11 * 2 / 5, paintText);
            } else {
                canvas.drawText(String.valueOf(value), (float) (startX + 8 * 0.9), startY - 11 * 2 / 4, paintText);

            }
        }
    }


    private float lastTouchXCircle;
    private float lastTouchYCircle;
    private float xcircle;
    private float ycircle;

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (this.getParent() != null) {
                    this.getParent().requestDisallowInterceptTouchEvent(true);
                }
                startTouch(event);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (this.getParent() != null) {
                    this.getParent().requestDisallowInterceptTouchEvent(true);
                }
                duringTouch(event);
                return true;

            case MotionEvent.ACTION_UP:
                if (this.getParent() != null) {
                    this.getParent().requestDisallowInterceptTouchEvent(true);
                }
                processTouch(event);
                return true;
            default:
                return false;
        }
    }

    private void duringTouch(final MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();

        switch (dialDirection) {
            //for left
            case 1:
                xcircle = eventX - centerX;
                ycircle = centerY - eventY;
                break;
            //for top
            case 2:
                xcircle = eventX - centerX;
                ycircle = centerY - eventY;
                break;
            //for right
            case 3:
                xcircle = centerX + eventX;
                ycircle = centerY - eventY;
                break;
            //for bottom
            case 4:
                xcircle = eventX - centerX;
                ycircle = centerY - eventY;
                break;

            default:
                // // TODO: 4/5/17 nothing
                break;
        }

        double originalAngle = Math.atan2(lastTouchYCircle, lastTouchXCircle);
        double newAngle = Math.atan2(ycircle, xcircle);

        switch (dialDirection) {
            //for left
            case 1:
                delta = originalAngle - newAngle;
                break;
            //for top
            case 2:
                delta = originalAngle - newAngle;
                break;
            //for right
            case 3:
                delta = newAngle - originalAngle;
                break;
            //for bottom
            case 4:
                delta = originalAngle - newAngle;
                break;

            default:
                //// TODO: 4/5/17 nothing
                break;
        }

        rotate(delta);
        touchState = TOUCH_STATE_SCROLL;
        processTouch(event);
    }

    /**
     * @param event when user starts touch so that there is initial touch of x and y
     *              initialising the velocity and adding movement
     */

    private void startTouch(final MotionEvent event) {
        // user is touching the list -> no more fling
        removeCallbacks(dynamicsRunnable);

        if (angleToCompare == 0) {
            //for left
            lastTouchXCircle = event.getX() - centerX;
            lastTouchYCircle = centerY - event.getY();
        } else if (angleToCompare == 90) {
            //for top
            lastTouchXCircle = event.getX() - centerX;
            lastTouchYCircle = centerY - event.getY();
        } else if (angleToCompare == 180) {
            //for right
            lastTouchXCircle = centerX + event.getX();
            lastTouchYCircle = centerY - event.getY();
        } else if (angleToCompare == 270) {
            //for bottom
            lastTouchXCircle = event.getX() - centerX;
            lastTouchYCircle = centerY - event.getY();
        }
        // obtain a velocity tracker and feed it its first event
        velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        // we don't know if it's a click or a scroll yet, but until we know
        // assume it's a click
        touchState = TOUCH_STATE_CLICK;
        // getting the event of dialview
//        if (onValueChangeListener != null) {
//            onValueChangeListener.onMotionEvent(event);
//        }
    }

    /**
     * @param delta method used to rotate the dial between the max angle and min angle theta
     */

    private void rotate(double delta) {
        currentTheta += delta;

        if (angleToCompare == 0) {
            if (currentTheta > minAngleTheta && currentTheta < (maxAngleTheta)) {
                invalidate();
                initTheta += delta;
                lastTouchXCircle = xcircle;
                lastTouchYCircle = ycircle;
            } else {
                if (currentTheta < minAngleTheta) {
                    currentTheta = minAngleTheta;
                } else if (currentTheta > maxAngleTheta) {
                    currentTheta = maxAngleTheta;
                }
            }
        } else if (angleToCompare == 180 || angleToCompare == 270 || angleToCompare == 90) {
            if ((currentTheta <= minAngleTheta + (angleToCompare * PI / 180)
                    && currentTheta >= (angleToCompare * PI / 180)
                    - (maxAngleTheta))) {
                invalidate();
                initTheta += delta;
                lastTouchXCircle = xcircle;
                lastTouchYCircle = ycircle;
            } else {
                if (angleToCompare == 90) {
                    if (currentTheta > PI / 2)
                        currentTheta = PI / 2;
                    else if (currentTheta < (angleToCompare * PI / 180) - maxAngleTheta)
                        currentTheta = (angleToCompare * PI / 180) - maxAngleTheta;
                } else if (angleToCompare == 180) {
                    if (currentTheta > PI) {
                        currentTheta = PI;
                    } else if (currentTheta < (angleToCompare * PI / 180) - maxAngleTheta) {
                        currentTheta = (angleToCompare * PI / 180) - maxAngleTheta;
                    }
                } else if (angleToCompare == 270) {
                    if (currentTheta > 3 * PI / 2)
                        currentTheta = 3 * PI / 2;
                    else if (currentTheta < (angleToCompare * PI / 180) - maxAngleTheta)
                        currentTheta = (angleToCompare * PI / 180) - maxAngleTheta;
                }
            }
        }
    }

    /**
     * @param event method used when user moves the dial in motion
     *              to add the velocity and calculate the velocity and
     */

    public boolean processTouch(final MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(event);
                break;

            case MotionEvent.ACTION_UP:
                float velocity = 0;
                if (touchState == TOUCH_STATE_SCROLL) {
                    velocityTracker.computeCurrentVelocity(RADIANS_PER_SECOND);
                    switch (dialDirection) {
                        //for left
                        case 1:
                            velocity = -1 * velocityTracker.getYVelocity();
                            break;
                        //for top
                        case 2:
                            velocity = -1 * velocityTracker.getXVelocity();
                            break;
                        //for right
                        case 3:
                            velocity = -1 * velocityTracker.getYVelocity();
                            break;
                        //for bottom
                        case 4:
                            velocity = -1 * velocityTracker.getXVelocity();
                            break;
                    }
                }
                endTouch(velocity);
                break;

            default:
                // todo nothing
                break;
        }
        return true;
    }

    private void endTouch(final float velocity) {
        // recycle the velocity tracker
        velocityTracker.recycle();
        velocityTracker = null;
        currentTime = System.nanoTime();
        if (angleToCompare == 0) {
            //for left
            initVelocity = -1 * velocity;
        } else if (angleToCompare == 90) {
            //for top
            initVelocity = velocity;
        } else if (angleToCompare == 180) {
            //for right
            initVelocity = velocity;
        } else if (angleToCompare == 270) {
            //for bottom
            initVelocity = -1 * velocity;
        }

        post(dynamicsRunnable);

        // reset touch state
        touchState = TOUCH_STATE_RESTING;
    }

    public interface OnDialValueChangeListener {
        void onDialValueChanged(String value, int maxValue);
    }
}

