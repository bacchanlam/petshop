/**
 * Auto-hide alerts after 5 seconds
 * Dùng cho tất cả success, danger, info alerts
 */
document.addEventListener("DOMContentLoaded", function () {
  // Tìm tất cả alerts (trừ những có dismiss button)
  const alerts = document.querySelectorAll(".alert:not(.alert-dismissible)");

  alerts.forEach((alert) => {
    // Nếu là success hoặc info, tự động thêm class alert-autohide
    if (
      alert.classList.contains("alert-success") ||
      alert.classList.contains("alert-info")
    ) {
      alert.classList.add("alert-autohide");
    }
  });

  // Cho các alert đã có class alert-autohide sẵn, clear nó sau 5s
  const autohideAlerts = document.querySelectorAll(".alert-autohide");
  autohideAlerts.forEach((alert) => {
    setTimeout(() => {
      alert.remove();
    }, 5000);
  });
});
