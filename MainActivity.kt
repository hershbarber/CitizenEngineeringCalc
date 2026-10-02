package com.example.citizencalc

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private var currentInput = ""
    private var lastResult = 0.0
    private var isNewInput = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.tvDisplay)

        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot
        )

        numberButtons.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener { append((it as Button).text.toString()) }
        }

        findViewById<Button>(R.id.btnPlus)?.setOnClickListener { appendOperator("+") }
        findViewById<Button>(R.id.btnMinus)?.setOnClickListener { appendOperator("-") }
        findViewById<Button>(R.id.btnMul)?.setOnClickListener { appendOperator("×") }
        findViewById<Button>(R.id.btnDiv)?.setOnClickListener { appendOperator("÷") }
        findViewById<Button>(R.id.btnPow)?.setOnClickListener { appendOperator("^") }
        findViewById<Button>(R.id.btnOpen)?.setOnClickListener { append("(") }
        findViewById<Button>(R.id.btnClose)?.setOnClickListener { append(")") }

        findViewById<Button>(R.id.btnSin)?.setOnClickListener { func("sin") }
        findViewById<Button>(R.id.btnCos)?.setOnClickListener { func("cos") }
        findViewById<Button>(R.id.btnTan)?.setOnClickListener { func("tan") }
        findViewById<Button>(R.id.btnLog)?.setOnClickListener { func("log") }
        findViewById<Button>(R.id.btnLn)?.setOnClickListener { func("ln") }
        findViewById<Button>(R.id.btnSqrt)?.setOnClickListener { func("sqrt") }
        findViewById<Button>(R.id.btnFact)?.setOnClickListener { func("fact") }
        findViewById<Button>(R.id.btnPi)?.setOnClickListener { append("π") }
        findViewById<Button>(R.id.btnE)?.setOnClickListener { append("e") }

        findViewById<Button>(R.id.btnDegRad)?.setOnClickListener {
            val btn = it as Button
            if (btn.text == "DEG") btn.text = "RAD" else btn.text = "DEG"
        }

        findViewById<Button>(R.id.btnAC)?.setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnDEL)?.setOnClickListener { deleteLast() }
        findViewById<Button>(R.id.btnEqual)?.setOnClickListener { calculate() }
    }

    private fun append(value: String) {
        if (isNewInput && value != ".") {
            currentInput = ""
            isNewInput = false
        }
        currentInput += value
        updateDisplay()
    }

    private fun appendOperator(op: String) {
        isNewInput = false
        currentInput += op
        updateDisplay()
    }

    private fun func(name: String) {
        isNewInput = false
        currentInput += name + "("
        updateDisplay()
    }

    private fun clearAll() {
        currentInput = ""
        lastResult = 0.0
        isNewInput = true
        tvDisplay.text = "0"
    }

    private fun deleteLast() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
        }
        updateDisplay()
    }

    private fun updateDisplay() {
        tvDisplay.text = if (currentInput.isEmpty()) "0" else currentInput
    }

    private fun calculate() {
        if (currentInput.isEmpty()) return
        try {
            val expr = preprocess(currentInput)
            val result = evaluate(expr)
            lastResult = result
            tvDisplay.text = formatResult(result)
            currentInput = formatResult(result)
            isNewInput = true
        } catch (e: Exception) {
            tvDisplay.text = "Error"
            isNewInput = true
        }
    }

    private fun preprocess(input: String): String {
        return input
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", PI.toString())
            .replace("e", E.toString())
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val rounded = kotlin.math.round(value * 1e12) / 1e12
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private fun evaluate(expr: String): Double {
        return parseExpression(expr.replace(" ", "").toList(), 0).first
    }

    private fun parseExpression(chars: List<Char>, index: Int): Pair<Double, Int> {
        var (value, i) = parseTerm(chars, index)
        var idx = i
        while (idx < chars.size) {
            when (chars[idx]) {
                '+' -> { val (v, ni) = parseTerm(chars, idx + 1); value += v; idx = ni }
                '-' -> { val (v, ni) = parseTerm(chars, idx + 1); value -= v; idx = ni }
                else -> break
            }
        }
        return value to idx
    }

    private fun parseTerm(chars: List<Char>, index: Int): Pair<Double, Int> {
        var (value, i) = parseFactor(chars, index)
        var idx = i
        while (idx < chars.size) {
            when (chars[idx]) {
                '*' -> { val (v, ni) = parseFactor(chars, idx + 1); value *= v; idx = ni }
                '/' -> { val (v, ni) = parseFactor(chars, idx + 1); value /= v; idx = ni }
                '^' -> { val (v, ni) = parseFactor(chars, idx + 1); value = value.pow(v); idx = ni }
                else -> break
            }
        }
        return value to idx
    }

    private fun parseFactor(chars: List<Char>, index: Int): Pair<Double, Int> {
        if (index >= chars.size) return 0.0 to index
        val ch = chars[index]
        return when {
            ch == '+' -> parseFactor(chars, index + 1)
            ch == '-' -> { val (v, i) = parseFactor(chars, index + 1); (-v) to i }
            ch == '(' -> {
                val (v, i) = parseExpression(chars, index + 1)
                v to (i + 1)
            }
            ch.isDigit() || ch == '.' -> parseNumber(chars, index)
            else -> parseFunction(chars, index)
        }
    }

    private fun parseNumber(chars: List<Char>, index: Int): Pair<Double, Int> {
        var i = index
        val sb = StringBuilder()
        while (i < chars.size && (chars[i].isDigit() || chars[i] == '.')) {
            sb.append(chars[i])
            i++
        }
        return sb.toString().toDouble() to i
    }

    private fun parseFunction(chars: List<Char>, index: Int): Pair<Double, Int> {
        val names = listOf("sin", "cos", "tan", "log", "ln", "sqrt", "fact")
        var name = ""
        var i = index
        while (i < chars.size && chars[i].isLetter()) {
            name += chars[i]
            i++
        }
        if (name in names) {
            val (arg, ni) = parseFactor(chars, i)
            val isDeg = findViewById<Button>(R.id.btnDegRad).text == "DEG"
            val v = when (name) {
                "sin" -> if (isDeg) sin(Math.toRadians(arg)) else sin(arg)
                "cos" -> if (isDeg) cos(Math.toRadians(arg)) else cos(arg)
                "tan" -> if (isDeg) tan(Math.toRadians(arg)) else tan(arg)
                "log" -> log10(arg)
                "ln" -> ln(arg)
                "sqrt" -> sqrt(arg)
                "fact" -> factorial(arg)
                else -> arg
            }
            return v to ni
        }
        return 0.0 to i
    }

    private fun factorial(n: Double): Double {
        if (n < 0 || n != floor(n)) return Double.NaN
        var r = 1.0
        for (i in 2..n.toInt()) r *= i
        return r
    }
}
