<template>
  <div class="captcha-wrapper">
    <div class="captcha-image-container" @click="refreshCaptcha">
      <canvas ref="captchaCanvas" :width="width" :height="height"></canvas>
    </div>
    <div class="captcha-type-switch" @click="toggleType" :title="`切换到${nextTypeLabel}`">
      <i :class="typeIcon"></i>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CaptchaImage',
  props: {
    width: {
      type: Number,
      default: 120
    },
    height: {
      type: Number,
      default: 40
    }
  },
  data() {
    return {
      captchaAnswer: '',
      captchaType: 'arithmetic' // arithmetic: 算术验证码, alphanumeric: 字母数字验证码
    }
  },
  computed: {
    typeIcon() {
      return this.captchaType === 'arithmetic' ? 'el-icon-key' : 'el-icon-edit'
    },
    nextTypeLabel() {
      return this.captchaType === 'arithmetic' ? '字母验证码' : '算术验证码'
    }
  },
  mounted() {
    this.refreshCaptcha()
  },
  methods: {
    refreshCaptcha() {
      this.$emit('refresh')
      this.drawCaptcha()
    },

    toggleType() {
      this.captchaType = this.captchaType === 'arithmetic' ? 'alphanumeric' : 'arithmetic'
      this.refreshCaptcha()
    },

    drawCaptcha() {
      const canvas = this.$refs.captchaCanvas
      const ctx = canvas.getContext('2d')

      // 清空画布
      ctx.clearRect(0, 0, this.width, this.height)

      // 绘制背景
      ctx.fillStyle = this.randomColor(230, 255)
      ctx.fillRect(0, 0, this.width, this.height)

      if (this.captchaType === 'arithmetic') {
        this.drawArithmeticCaptcha(ctx)
      } else {
        this.drawAlphanumericCaptcha(ctx)
      }

      // 绘制干扰线
      for (let i = 0; i < 5; i++) {
        ctx.strokeStyle = this.randomColor(100, 200)
        ctx.lineWidth = 1
        ctx.beginPath()
        ctx.moveTo(Math.random() * this.width, Math.random() * this.height)
        ctx.lineTo(Math.random() * this.width, Math.random() * this.height)
        ctx.stroke()
      }

      // 绘制干扰点
      for (let i = 0; i < 30; i++) {
        ctx.fillStyle = this.randomColor(150, 220)
        ctx.beginPath()
        ctx.arc(
          Math.random() * this.width,
          Math.random() * this.height,
          1,
          0,
          2 * Math.PI
        )
        ctx.fill()
      }

      // 通知父组件答案
      this.$emit('answer-change', this.captchaAnswer)
    },

    drawArithmeticCaptcha(ctx) {
      // 生成算术题
      const operators = ['+', '-', '×']
      const operator = operators[Math.floor(Math.random() * operators.length)]
      let num1, num2, question

      switch (operator) {
        case '+':
          num1 = Math.floor(Math.random() * 20) + 1
          num2 = Math.floor(Math.random() * 20) + 1
          this.captchaAnswer = String(num1 + num2)
          question = `${num1}+${num2}=?`
          break
        case '-':
          num1 = Math.floor(Math.random() * 20) + 5
          num2 = Math.floor(Math.random() * num1)
          this.captchaAnswer = String(num1 - num2)
          question = `${num1}-${num2}=?`
          break
        case '×':
          num1 = Math.floor(Math.random() * 10) + 1
          num2 = Math.floor(Math.random() * 10) + 1
          this.captchaAnswer = String(num1 * num2)
          question = `${num1}×${num2}=?`
          break
      }

      // 绘制文字
      const fontSize = Math.floor(this.height * 0.5)
      ctx.font = `bold ${fontSize}px Arial`
      ctx.textBaseline = 'middle'

      // 计算文字宽度，居中显示
      const textWidth = ctx.measureText(question).width
      const startX = (this.width - textWidth) / 2

      // 逐个字符绘制，带随机旋转和颜色
      let currentX = startX
      for (let i = 0; i < question.length; i++) {
        ctx.save()
        const char = question[i]
        const charWidth = ctx.measureText(char).width

        // 随机旋转角度
        const angle = (Math.random() - 0.5) * 0.2
        const centerX = currentX + charWidth / 2
        const centerY = this.height / 2

        ctx.translate(centerX, centerY)
        ctx.rotate(angle)

        ctx.fillStyle = this.randomColor(50, 150)
        ctx.fillText(char, -charWidth / 2, 0)

        ctx.restore()
        currentX += charWidth
      }
    },

    drawAlphanumericCaptcha(ctx) {
      // 生成字母数字验证码
      const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
      let code = ''

      for (let i = 0; i < 4; i++) {
        code += chars[Math.floor(Math.random() * chars.length)]
      }

      this.captchaAnswer = code

      // 绘制文字
      const fontSize = Math.floor(this.height * 0.6)
      ctx.font = `bold ${fontSize}px Arial`
      ctx.textBaseline = 'middle'

      // 计算文字宽度，居中显示
      const textWidth = ctx.measureText(code).width
      const startX = (this.width - textWidth) / 2
      const charWidth = textWidth / code.length

      // 逐个字符绘制，带随机旋转和颜色
      for (let i = 0; i < code.length; i++) {
        ctx.save()
        const char = code[i]

        // 随机旋转角度
        const angle = (Math.random() - 0.5) * 0.5
        const centerX = startX + i * charWidth + charWidth / 2
        const centerY = this.height / 2

        ctx.translate(centerX, centerY)
        ctx.rotate(angle)

        ctx.fillStyle = this.randomColor(50, 150)
        ctx.fillText(char, -charWidth / 2, 0)

        ctx.restore()
      }
    },

    randomColor(min, max) {
      const r = Math.floor(Math.random() * (max - min) + min)
      const g = Math.floor(Math.random() * (max - min) + min)
      const b = Math.floor(Math.random() * (max - min) + min)
      return `rgb(${r},${g},${b})`
    }
  }
}
</script>

<style scoped>
.captcha-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.captcha-image-container {
  display: inline-block;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  transition: all 0.3s;
}

.captcha-image-container:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.captcha-image-container:active {
  transform: scale(0.98);
}

.captcha-type-switch {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  cursor: pointer;
  background-color: #f5f7fa;
  transition: all 0.3s;
  color: #606266;
}

.captcha-type-switch:hover {
  background-color: #667eea;
  color: white;
}

.captcha-type-switch:active {
  transform: scale(0.9);
}

.captcha-type-switch i {
  font-size: 16px;
}

canvas {
  display: block;
}
</style>
