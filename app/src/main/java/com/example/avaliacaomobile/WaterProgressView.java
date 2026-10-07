package com.example.avaliacaomobile;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class WaterProgressView extends View {

    private static final int ALERT_COLOR = Color.parseColor("#D32F2F"); // vermelho: meta ultrapassada
    private static final int TRACK_COLOR = Color.parseColor("#E0E0E0"); // cinza: parte restante

    private int progressColor = Color.parseColor("#2196F3");
    private int textColor = Color.BLACK;
    private int maxValue = 2000; // meta diária em ml

    private int consumed = 0;         // consumo acumulado em ml (valor real, sem limite)
    private float shownPercent = 0f;  // percentual exibido no momento (muda durante a animação)
    private boolean showAbsolute = false; // false = "75%", true = "1800/2000 ml"
    private ValueAnimator animator;

    // Textos fixos vindos de strings.xml (internacionalização), lidos uma única vez
    private final String titleText;
    private final String alertText;

    // Criados uma única vez (nunca dentro do onDraw)
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint alertPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    public WaterProgressView(Context context) {
        this(context, null);
    }

    public WaterProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        titleText = context.getString(R.string.water_title);
        alertText = context.getString(R.string.water_exceeded_short);

        // Lê os atributos declarados em attrs.xml
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaterProgressView);
        try {
            progressColor = a.getColor(R.styleable.WaterProgressView_progressColor, progressColor);
            textColor = a.getColor(R.styleable.WaterProgressView_textColor, textColor);
            maxValue = a.getInt(R.styleable.WaterProgressView_maxValue, maxValue);
        } finally {
            a.recycle(); // devolve o TypedArray ao sistema
        }
        if (maxValue <= 0) maxValue = 2000; // evita divisão por zero

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(TRACK_COLOR);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        titlePaint.setTextAlign(Paint.Align.CENTER);

        alertPaint.setTextAlign(Paint.Align.CENTER);
        alertPaint.setTypeface(Typeface.DEFAULT_BOLD);
        alertPaint.setColor(ALERT_COLOR);

        // Clique: alterna percentual/absoluto sem alterar o consumo
        setOnClickListener(v -> {
            showAbsolute = !showAbsolute;
            invalidate();
        });
    }

    public void setProgress(int percent) {
        percent = Math.max(0, percent);
        consumed = Math.round(percent * maxValue / 100f);
        animateTo(percent);
    }

    public void setConsumed(int ml) {
        consumed = Math.max(0, ml);
        animateTo(consumed * 100f / maxValue);
    }

    public void setMaxValue(int max) {
        if (max <= 0) return; // meta inválida é ignorada
        maxValue = max;
        animateTo(consumed * 100f / maxValue);
    }

    private void animateTo(float targetPercent) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(shownPercent, targetPercent);
        animator.setDuration(700);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            shownPercent = (float) a.getAnimatedValue();
            invalidate(); // pede para o Android chamar onDraw de novo
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Se o layout usar wrap_content, assume 200dp
        int def = (int) (200 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(def, widthSpec), resolveSize(def, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float offY = (h - size) / 2f;
        float stroke = size * 0.08f;
        float cx = w / 2f;
        float cy = offY + size * 0.54f;
        float radius = size * 0.34f;
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // O alerta depende dos VALORES (consumo > meta), não da animação
        boolean exceeded = consumed > maxValue;

        trackPaint.setStrokeWidth(stroke);
        progressPaint.setStrokeWidth(stroke);
        progressPaint.setColor(exceeded ? ALERT_COLOR : progressColor);

        titlePaint.setColor(textColor);
        titlePaint.setTextSize(size * 0.075f);
        canvas.drawText(titleText, cx, offY + size * 0.09f, titlePaint);

        canvas.drawCircle(cx, cy, radius, trackPaint);

        // Só o ARCO é limitado a 360° (começa no topo, -90°); o texto mostra o percentual real
        float sweep = Math.min(shownPercent, 100f) / 100f * 360f;
        if (sweep > 0) {
            canvas.drawArc(arcRect, -90, sweep, false, progressPaint);
        }

        // Texto com números: usa string com formato (%1$d, %2$d), preenchida a cada desenho
        String text;
        if (showAbsolute) {
            text = getContext().getString(R.string.water_absolute_format,
                    Math.round(shownPercent * maxValue / 100f), maxValue);
        } else {
            text = getContext().getString(R.string.water_percent_format, Math.round(shownPercent));
        }
        textPaint.setColor(exceeded ? ALERT_COLOR : textColor);
        textPaint.setTextSize(size * (showAbsolute ? 0.11f : 0.16f));
        // Reduz a fonte se o texto não couber dentro do anel
        float maxTextWidth = (radius - stroke) * 2f;
        float textWidth = textPaint.measureText(text);
        if (textWidth > maxTextWidth) {
            textPaint.setTextSize(textPaint.getTextSize() * maxTextWidth / textWidth);
        }
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f; // centraliza na vertical
        canvas.drawText(text, cx, textY, textPaint);

        // Aviso dentro do componente quando a meta é ultrapassada
        if (exceeded) {
            alertPaint.setTextSize(size * 0.045f);
            canvas.drawText(alertText, cx, cy + size * 0.17f, alertPaint);
        }
    }
}