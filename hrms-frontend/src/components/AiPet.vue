<template>
  <svg
    class="ai-pet"
    :width="size"
    :height="size"
    viewBox="0 0 64 64"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <g class="pet-float">
      <!-- 金色描边星 -->
      <polygon
        class="pet-star-outline"
        points="32,13 39.9,25.1 53.9,28.9 44.8,40.2 45.5,54.6 32,49.5 18.5,54.6 19.2,40.2 10.1,28.9 24.1,25.1"
      />
      <!-- 深蓝星身 -->
      <polygon
        class="pet-star-body"
        points="32,13 39.9,25.1 53.9,28.9 44.8,40.2 45.5,54.6 32,49.5 18.5,54.6 19.2,40.2 10.1,28.9 24.1,25.1"
      />
      <!-- 头顶绿色小帆 -->
      <path class="pet-sail" d="M32 13 Q 36 4 44 5 Q 43 13 32 13 Z" />
      <path class="pet-sail-vein" d="M33.5 12 Q 37.5 8.5 41.5 7.5" />
      <!-- 眼睛（带眨眼动画） -->
      <g class="pet-eyes">
        <ellipse cx="26" cy="34" rx="4.4" ry="5.4" fill="#fff" />
        <ellipse cx="38" cy="34" rx="4.4" ry="5.4" fill="#fff" />
        <circle cx="26.8" cy="35" r="2.6" fill="#16294a" />
        <circle cx="37.2" cy="35" r="2.6" fill="#16294a" />
        <circle cx="25.9" cy="33.2" r="1" fill="#fff" />
        <circle cx="36.3" cy="33.2" r="1" fill="#fff" />
      </g>
      <!-- 微笑 -->
      <path
        class="pet-smile"
        d="M29 42 Q 32 45 35 42"
      />
      <!-- 腮红 -->
      <ellipse class="pet-blush" cx="20.5" cy="39.5" rx="2.4" ry="1.4" />
      <ellipse class="pet-blush" cx="43.5" cy="39.5" rx="2.4" ry="1.4" />
    </g>
  </svg>
</template>

<script setup>
defineProps({
  size: {
    type: Number,
    default: 32
  }
})
</script>

<style scoped lang="scss">
.ai-pet {
  display: block;
  overflow: visible;
}

.pet-star-outline {
  fill: #f5a623;
  stroke: #f5a623;
  stroke-width: 7;
  stroke-linejoin: round;
}

.pet-star-body {
  fill: #1e3a5f;
  stroke: #1e3a5f;
  stroke-width: 4;
  stroke-linejoin: round;
}

.pet-sail {
  fill: #3cb54a;
  stroke: #2e9440;
  stroke-width: 1;
  stroke-linejoin: round;
}

.pet-sail-vein {
  fill: none;
  stroke: #eaf7ea;
  stroke-width: 1.2;
  stroke-linecap: round;
  opacity: 0.9;
}

.pet-eyes {
  transform-box: fill-box;
  transform-origin: center;
  animation: pet-blink 3.6s infinite;
}

.pet-smile {
  fill: none;
  stroke: #ffffff;
  stroke-width: 1.8;
  stroke-linecap: round;
}

.pet-blush {
  fill: #ff8fa8;
  opacity: 0.75;
}

.pet-float {
  animation: pet-float 2.8s ease-in-out infinite;
  transform-box: fill-box;
  transform-origin: center;
}

/* 悬停时欢快摇摆 */
.ai-pet:hover .pet-float {
  animation: pet-wiggle 0.5s ease-in-out infinite;
}

@keyframes pet-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

@keyframes pet-blink {
  0%,
  91%,
  100% {
    transform: scaleY(1);
  }
  94% {
    transform: scaleY(0.12);
  }
  97% {
    transform: scaleY(1);
  }
}

@keyframes pet-wiggle {
  0%,
  100% {
    transform: rotate(-4deg);
  }
  50% {
    transform: rotate(4deg);
  }
}
</style>
