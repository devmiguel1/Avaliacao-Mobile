package com.example.waterapp;

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

import com.example.avaliacaomobile.R;

/**
 * View customizada que mostra o consumo diário de água como um anel de progresso.
 * Tudo é desenhado manualmente no Canvas (onDraw).
 *
 * Uso no XML:
 * <com.example.waterapp.WaterProgressView
 *     android:layout_width="220dp" android:layout_height="220dp"
 *     app:progressColor="#2196F3" app:textColor="#000000" app:maxValue="2000" />
 */
public class WaterProgressView extends View {

    private static final int ALERT_COLOR = Color.parseColor("#D32F2F");      // vermelho: meta ultrapassada
    private static final int ALERT_DARK_COLOR = Color.parseColor("#7F0000"); // vermelho escuro: volta extra
    private static final int TRACK_COLOR = Color.parseColor("#E0E0E0");      // cinza: parte restante

    // Valores configuráveis (XML ou código)
    private int progressColor = Color.parseColor("#2196F3");
    private int textColor = Color.BLACK;
    private int maxValue = 2000; // meta diária em ml

    // Estado
    private int consumed = 0;         // consumo acumulado em ml (valor real, sem limite)
    private float shownPercent = 0f;  // percentual exibido no momento (muda durante a animação)
    private boolean showAbsolute = false; // false = modo percentual, true = modo "1800/2000 ml"
    private ValueAnimator animator;

    // Objetos de desenho criados uma única vez (nunca dentro do onDraw)
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overflowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint alertPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    public WaterProgressView(Context context) {
        this(context, null);
    }

    public WaterProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Lê os atributos declarados em attrs.xml
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaterProgressView);
        try {
            progressColor = a.getColor(R.styleable.WaterProgressView_progressColor, progressColor);
            textColor = a.getColor(R.styleable.WaterProgressView_textColor, textColor);
            maxValue = a.getInt(R.styleable.WaterProgressView_maxValue, maxValue);
        } finally {
            a.recycle(); // o TypedArray é compartilhado pelo sistema, precisa ser devolvido
        }
        if (maxValue <= 0) maxValue = 2000; // evita divisão por zero

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(TRACK_COLOR);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        overflowPaint.setStyle(Paint.Style.STROKE);
        overflowPaint.setStrokeCap(Paint.Cap.ROUND);
        overflowPaint.setColor(ALERT_DARK_COLOR);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        titlePaint.setTextAlign(Paint.Align.CENTER);

        alertPaint.setTextAlign(Paint.Align.CENTER);
        alertPaint.setTypeface(Typeface.DEFAULT_BOLD);
        alertPaint.setColor(ALERT_COLOR);

        // Clique: alterna entre modo percentual e modo absoluto (o consumo não é alterado)
        setOnClickListener(v -> {
            showAbsolute = !showAbsolute;
            invalidate();
        });
    }

    // ---------------------------------------------------------------- API pública

    /**
     * Define o progresso em PERCENTUAL da meta (pode passar de 100).
     * Atualiza com animação suave.
     */
    public void setProgress(int percent) {
        percent = Math.max(0, percent);
        consumed = Math.round(percent * maxValue / 100f);
        animateTo(percent);
    }

    /** Define o consumo acumulado em ml (valor exato) e anima o anel. */
    public void setConsumed(int ml) {
        consumed = Math.max(0, ml);
        animateTo(consumed * 100f / maxValue);
    }

    /** Altera a meta diária em ml em tempo de execução. O consumo é mantido. */
    public void setMaxValue(int max) {
        if (max <= 0) return; // meta inválida é ignorada
        maxValue = max;
        animateTo(consumed * 100f / maxValue);
    }

    public int getMaxValue() {
        return maxValue;
    }

    // ---------------------------------------------------------------- animação

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
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    // ---------------------------------------------------------------- medição

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        // Se o layout usar wrap_content, assume 200dp
        int def = (int) (200 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(def, widthSpec), resolveSize(def, heightSpec));
    }

    // ---------------------------------------------------------------- desenho

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);          // o desenho cabe no menor lado
        float offY = (h - size) / 2f;         // centraliza na vertical
        float stroke = size * 0.08f;
        float cx = w / 2f;
        float cy = offY + size * 0.54f;
        float radius = size * 0.34f;
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // O alerta depende dos VALORES (consumo > meta), não da animação
        boolean exceeded = consumed > maxValue;

        trackPaint.setStrokeWidth(stroke);
        progressPaint.setStrokeWidth(stroke);
        overflowPaint.setStrokeWidth(stroke);
        progressPaint.setColor(exceeded ? ALERT_COLOR : progressColor);

        // Título
        titlePaint.setColor(textColor);
        titlePaint.setTextSize(size * 0.075f);
        canvas.drawText("Consumo diário de água", cx, offY + size * 0.09f, titlePaint);

        // 1) Trilho: parte restante da meta
        canvas.drawCircle(cx, cy, radius, trackPaint);

        // 2) Progresso realizado (começa no topo, -90°). O ARCO é limitado a 360°,
        //    mas o percentual mostrado no texto NÃO é limitado.
        float sweep = Math.min(shownPercent, 100f) / 100f * 360f;
        if (sweep > 0) {
            canvas.drawArc(arcRect, -90, sweep, false, progressPaint);
        }

        // 3) Acima de 100%: segunda volta em vermelho escuro por cima
        if (shownPercent > 100f) {
            float extra = Math.min(shownPercent - 100f, 100f) / 100f * 360f;
            canvas.drawArc(arcRect, -90, extra, false, overflowPaint);
        }

        // 4) Texto central: percentual ou absoluto, conforme o modo
        String text;
        if (showAbsolute) {
            text = Math.round(shownPercent * maxValue / 100f) + "/" + maxValue + " ml";
        } else {
            text = Math.round(shownPercent) + "%";
        }
        textPaint.setColor(exceeded ? ALERT_COLOR : textColor);
        textPaint.setTextSize(size * (showAbsolute ? 0.11f : 0.16f));
        // Se o texto não couber dentro do anel, reduz o tamanho
        float maxTextWidth = (radius - stroke) * 2f;
        float textWidth = textPaint.measureText(text);
        if (textWidth > maxTextWidth) {
            textPaint.setTextSize(textPaint.getTextSize() * maxTextWidth / textWidth);
        }
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f; // centraliza na vertical
        canvas.drawText(text, cx, textY, textPaint);

        // 5) Aviso dentro do componente quando a meta é ultrapassada
        if (exceeded) {
            alertPaint.setTextSize(size * 0.045f);
            canvas.drawText("Meta ultrapassada!", cx, cy + size * 0.17f, alertPaint);
        }
    }
}
