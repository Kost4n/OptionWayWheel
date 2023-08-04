package wheel.`fun`.game

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import wheel.`fun`.game.databinding.ActivityPlugBinding
import java.io.File
import java.util.Random


class PlugActivity: AppCompatActivity() {
    private lateinit var binding: ActivityPlugBinding
    private lateinit var rul: ImageView
    private lateinit var random: Random
    private var isRed = false
    private var isBlack = false
    private var score = 0
    private var bet = 10
    private val fileScore = "file_score"
    private var old_deegre = 0
    private var deegre = 10
    private val FACTOR = 4.86f
    private val numbers = arrayOf(
        "32 RED", "15 BLACK", "19 RED", "4 BLACK",
        "21 RED", "2 BLACK", "25 RED", "17 BLACK", "34 RED",
        "6 BLACK", "27 RED", "13 BLACK", "36 RED", "11 BLACK", "30 RED",
        "8 BLACK", "23 RED", "10 BLACK", "5 RED", "24 BLACK", "16 RED", "33 BLACK",
        "1 RED", "20 BLACK", "14 RED", "31 BLACK", "9 RED", "22 BLACK", "18 RED",
        "29 BLACK", "7 RED", "28 BLACK", "12 RED", "35 BLACK", "3 RED", "26 BLACK", "0"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlugBinding.inflate(layoutInflater)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        init()
        binding.buttonRed.setOnClickListener {
            if (!isBlack) {
                isRed = true
                it.alpha = 0.65F
            }
        }
        binding.buttonBlack.setOnClickListener {
            if (!isRed) {
                isBlack = true
                it.alpha = 0.65F
            }
        }
        binding.buttonPlus.setOnClickListener {
            if (score > 0 && score > bet) {
                bet += 10
                Log.d("bet plusbutton", "$bet  $score")
                binding.textBet.text = bet.toString()
            }
        }
        binding.buttonMinus.setOnClickListener {
            if (bet >= 20) {
                bet -= 10
                binding.textBet.text = bet.toString()
                Log.d("bet plusbutton", "$bet  $score")
            }
        }
    }

    fun onClickStart(view: View?) {
        if ((isRed || isBlack) && score >= bet) {
            score -= bet
            binding.textScore.text = score.toString()
            old_deegre = deegre % 360
            deegre = random.nextInt(3600) + 720
            val rotate = RotateAnimation(
                old_deegre.toFloat(), deegre.toFloat(), RotateAnimation.RELATIVE_TO_SELF,
                0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f
            )
            rotate.duration = 3600
            rotate.fillAfter = true
            rotate.interpolator = DecelerateInterpolator()
            rotate.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {

                }

                override fun onAnimationEnd(animation: Animation) {
                    isWin(getResult(360 - deegre % 360))
                    isRed = false
                    isBlack = false
                    binding.buttonRed.alpha = 1F
                    binding.buttonBlack.alpha = 1F
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            rul.startAnimation(rotate)
        }
    }

    private fun init() {
        rul = binding.rul
        random = Random()
        score = if (isHave(fileScore)) {
            textFile(fileScore).toInt()
        } else {
            100
        }
        if (score == 0) {
            score = 100
        }
        binding.textScore.text = score.toString()
    }

    private fun textFile(fileName: String) =
        File(applicationContext.filesDir, fileName)
            .bufferedReader()
            .use { it.readText() }

    private fun isHave(fileName: String): Boolean =
        File(applicationContext.filesDir, fileName)
            .exists()

    private fun saveFile(text: String, fileName: String) {
        applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE)
            .use { it.write(text.toByteArray()) }
    }

    private fun getResult(deegre: Int): String {
        var text = ""
        var factor_x = 1
        var factor_y = 3
        for (i in 0..36) {
            if (deegre >= FACTOR * factor_x && deegre < FACTOR * factor_y) {
                text = numbers[i]
            }
            factor_x += 2
            factor_y += 2
        }
        if (deegre >= FACTOR * 73 && deegre < 360 || deegre >= 0 && deegre < FACTOR * 1)
            text = numbers[numbers.size - 1]
        return text
    }

    private fun isWin(number: String) {
        val betNumber = binding.selectNumber.text.toString()
        var winBet = 1
        if (number.contains("RED") && isRed) {
            if (betNumber != "") {
                if (number.contains(betNumber)) {
                    winBet = (bet * 2.5).toInt()
                    showResult("Win!", "Your winnings: ", winBet)
                } else {
                    showResult("Loss")
                }
            } else {
                winBet = (bet * 1.5).toInt()
                showResult("Win!", "Your winnings: ", winBet)
            }
            score += winBet
        }
        else if (number.contains("BLACK") && isBlack) {
            if (betNumber != "") {
                if (number.contains(betNumber)) {
                    winBet = (bet * 2.5).toInt()
                    showResult("Win!", "Your winnings: ", winBet)
                } else {
                    showResult("Loss")
                }
            } else {
                winBet = (bet * 1.5).toInt()
                showResult("Win!", "Your winnings: ", winBet)
            }
            score += winBet
        } else if (number == "0") {
            winBet = bet * 4
            score += winBet
            showResult("Extrawin!", "Your winnings: ", winBet)
        } else {
            showResult("Loss")
        }
        binding.textScore.text = score.toString()
    }

    private fun showResult(title: String, body: String = "", winBet: Int = 0) {
        val builder = AlertDialog.Builder(binding.root.context)
            .setTitle(title)
            .setPositiveButton("Ok") {
                dialog, _ -> dialog.dismiss()
            }
            .setCancelable(false)
        if (winBet != 0) {
            builder.setMessage("$body$winBet points")
        } else {
            builder.setMessage(body)
        }
        builder.show()
    }

    override fun onPause() {
        super.onPause()
        saveFile(score.toString(), fileScore)
    }
}