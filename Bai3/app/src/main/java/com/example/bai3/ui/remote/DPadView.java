package com.example.bai3.ui.remote;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * DPadView — renders a D-pad as an annular sector ring.
 *
 * <p>The ring is divided into 4 equal 90° sectors (Up / Right / Down / Left),
 * separated by thin gaps. A circular "OK" button sits in the center.
 * Tapping any sector fires the corresponding direction callback; tapping the
 * center fires the OK callback.
 *
 * <p>Sector layout (angles in Android canvas coords, Y-down):
 * <ul>
 *   <li>UP    : -135° → -45°   (top)</li>
 *   <li>RIGHT :  -45° →  45°   (right)</li>
 *   <li>DOWN  :   45° → 135°   (bottom)</li>
 *   <li>LEFT  :  135° → 225°   (left)</li>
 * </ul>
 */
public class DPadView extends View {

    // ─── Callbacks ────────────────────────────────────────────────────────────
    public interface DPadListener {
        void onUp();
        void onDown();
        void onLeft();
        void onRight();
        void onOk();
    }

    private DPadListener listener;

    public void setDPadListener(DPadListener l) { this.listener = l; }

    // ─── Geometry ─────────────────────────────────────────────────────────────
    /** Fraction of the total radius occupied by the inner (center) circle. */
    private static final float INNER_RATIO  = 0.36f;
    /** Gap between sectors, in degrees. */
    private static final float GAP_DEG      = 0f;
    /** Border stroke width in dp. */
    private static final float STROKE_DP    = 1.5f;

    private float cx, cy, outerR, innerR;

    // ─── Paint ────────────────────────────────────────────────────────────────
    private final Paint sectorPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pressedPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint okFillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint okTextPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ─── State ────────────────────────────────────────────────────────────────
    /** Which sector is currently pressed: 0=none, 1=UP, 2=RIGHT, 3=DOWN, 4=LEFT, 5=OK */
    private int pressedSector = 0;

    // ─── Colours (set in init) ─────────────────────────────────────────────────
    private static final int COLOR_SECTOR   = 0xFF1E1E2E;
    private static final int COLOR_PRESSED  = 0xFF2A2A3E;
    private static final int COLOR_BORDER   = 0xFF00C2FF;   // primary_accent
    private static final int COLOR_OK_FILL  = 0xFF0A3D5C;
    private static final int COLOR_OK_GLOW  = 0x4400C2FF;
    private static final int COLOR_ARROW    = 0xFFFFFFFF;
    private static final int COLOR_OK_TEXT  = 0xFF00C2FF;

    // ─── Constructors ─────────────────────────────────────────────────────────
    public DPadView(Context context) {
        super(context); init();
    }

    public DPadView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }

    public DPadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        float density = getContext().getResources().getDisplayMetrics().density;
        float strokePx = STROKE_DP * density;

        sectorPaint.setColor(COLOR_SECTOR);
        sectorPaint.setStyle(Paint.Style.FILL);

        pressedPaint.setColor(COLOR_PRESSED);
        pressedPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(COLOR_BORDER);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(strokePx);

        okFillPaint.setColor(COLOR_OK_FILL);
        okFillPaint.setStyle(Paint.Style.FILL);

        okTextPaint.setColor(COLOR_OK_TEXT);
        okTextPaint.setTextAlign(Paint.Align.CENTER);
        okTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        // text size set in onSizeChanged

        arrowPaint.setColor(COLOR_ARROW);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAntiAlias(true);

        setBackground(null);
    }

    // ─── Size ─────────────────────────────────────────────────────────────────
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Force square
        int size = Math.min(
                MeasureSpec.getSize(widthSpec),
                MeasureSpec.getSize(heightSpec)
        );
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        cx = w / 2f;
        cy = h / 2f;
        float minDim = Math.min(w, h);
        outerR = minDim / 2f - 2f;    // 2px padding so border isn't clipped
        innerR = outerR * INNER_RATIO;

        float textSizePx = innerR * 0.45f;
        okTextPaint.setTextSize(textSizePx);
    }

    // ─── Draw ─────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawSector(canvas, 1, -135f, 90f);   // UP
        drawSector(canvas, 2,  -45f, 90f);   // RIGHT
        drawSector(canvas, 3,   45f, 90f);   // DOWN
        drawSector(canvas, 4,  135f, 90f);   // LEFT

        drawOkButton(canvas);
    }

    /**
     * Draw one annular sector (ring segment).
     *
     * @param sectorId  1=UP, 2=RIGHT, 3=DOWN, 4=LEFT
     * @param startDeg  start angle of the sector (Android canvas, Y-down)
     * @param sweepDeg  angular width of the sector
     */
    private void drawSector(Canvas canvas, int sectorId, float startDeg, float sweepDeg) {
        float halfGap = GAP_DEG / 2f;
        float s = startDeg + halfGap;
        float sw = sweepDeg - GAP_DEG;

        Path path = buildAnnularSectorPath(s, sw, outerR, innerR);

        // Fill
        canvas.drawPath(path, pressedSector == sectorId ? pressedPaint : sectorPaint);
        // Border
        canvas.drawPath(path, borderPaint);

        // Arrow icon in center of sector
        drawArrow(canvas, sectorId, s + sw / 2f);
    }

    /**
     * Build the path for an annular sector (donut slice).
     * The path traces: outer arc → line to inner → inner arc (reversed) → close.
     */
    private Path buildAnnularSectorPath(float startDeg, float sweepDeg, float outerRadius, float innerRadius) {
        Path path = new Path();

        RectF outerRect = new RectF(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius);
        RectF innerRect = new RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius);

        // Start point on outer arc
        double startRad = Math.toRadians(startDeg);
        float startX = cx + (float)(outerRadius * Math.cos(startRad));
        float startY = cy + (float)(outerRadius * Math.sin(startRad));
        path.moveTo(startX, startY);

        // Outer arc (clockwise)
        path.arcTo(outerRect, startDeg, sweepDeg, false);

        // Line from outer end to inner end
        double endRad = Math.toRadians(startDeg + sweepDeg);
        float innerEndX = cx + (float)(innerRadius * Math.cos(endRad));
        float innerEndY = cy + (float)(innerRadius * Math.sin(endRad));
        path.lineTo(innerEndX, innerEndY);

        // Inner arc (counter-clockwise back to start)
        path.arcTo(innerRect, startDeg + sweepDeg, -sweepDeg, false);

        path.close();
        return path;
    }

    /**
     * Draw a small triangle arrow pointing in the sector direction.
     */
    private void drawArrow(Canvas canvas, int sectorId, float midAngleDeg) {
        // Place arrow at ~70% of the radial distance between inner and outer
        float arrowR = innerR + (outerR - innerR) * 0.52f;
        double angleRad = Math.toRadians(midAngleDeg);
        float ax = cx + (float)(arrowR * Math.cos(angleRad));
        float ay = cy + (float)(arrowR * Math.sin(angleRad));

        float size = (outerR - innerR) * 0.28f;

        Path arrow = new Path();
        // Rotate based on direction
        // sectorId: 1=UP (midAngle≈-90), 2=RIGHT (midAngle≈0), 3=DOWN (midAngle≈90), 4=LEFT (midAngle≈180)
        double perpRad = angleRad + Math.PI / 2;
        float px = (float) Math.cos(perpRad);
        float py = (float) Math.sin(perpRad);
        double backRad = angleRad + Math.PI;
        float bx = (float) Math.cos(backRad);
        float by = (float) Math.sin(backRad);

        // Triangle tip points toward midAngle direction
        float tipX = ax + (float)(Math.cos(angleRad)) * size;
        float tipY = ay + (float)(Math.sin(angleRad)) * size;
        float base1X = ax + px * size * 0.65f + bx * size * 0.3f;
        float base1Y = ay + py * size * 0.65f + by * size * 0.3f;
        float base2X = ax - px * size * 0.65f + bx * size * 0.3f;
        float base2Y = ay - py * size * 0.65f + by * size * 0.3f;

        arrow.moveTo(tipX, tipY);
        arrow.lineTo(base1X, base1Y);
        arrow.lineTo(base2X, base2Y);
        arrow.close();

        canvas.drawPath(arrow, arrowPaint);
    }

    /**
     * Draw the center OK circle.
     */
    private void drawOkButton(Canvas canvas) {
        // Fill
        Paint fill = (pressedSector == 5) ? pressedPaint : okFillPaint;
        canvas.drawCircle(cx, cy, innerR, fill);

        // Border
        canvas.drawCircle(cx, cy, innerR, borderPaint);

        // "OK" text
        Paint.FontMetrics fm = okTextPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText("OK", cx, textY, okTextPaint);
    }

    // ─── Touch ────────────────────────────────────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();
        int sector = hitTest(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressedSector = sector;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                int released = pressedSector;
                pressedSector = 0;
                invalidate();
                if (released != 0 && released == sector) {
                    fireSector(released);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                pressedSector = 0;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Determine which sector the touch point falls in.
     * Returns 0 if outside the ring, 5 for the center OK, 1-4 for directions.
     */
    private int hitTest(float x, float y) {
        float dx = x - cx;
        float dy = y - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < innerR) return 5;           // center OK
        if (dist > outerR) return 0;           // outside ring

        // Angle in degrees, from -180 to 180 (Y-down)
        double deg = Math.toDegrees(Math.atan2(dy, dx));

        // UP: -135 to -45
        if (deg >= -135 && deg < -45) return 1;
        // RIGHT: -45 to 45
        if (deg >= -45 && deg < 45)   return 2;
        // DOWN: 45 to 135
        if (deg >= 45  && deg < 135)  return 3;
        // LEFT: 135 to 180 OR -180 to -135
        return 4;
    }

    private void fireSector(int sector) {
        if (listener == null) return;
        switch (sector) {
            case 1: listener.onUp();    break;
            case 2: listener.onRight(); break;
            case 3: listener.onDown();  break;
            case 4: listener.onLeft();  break;
            case 5: listener.onOk();    break;
        }
    }
}
