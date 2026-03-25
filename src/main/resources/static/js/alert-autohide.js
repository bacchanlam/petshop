
document.addEventListener("DOMContentLoaded", function () {
  const alerts = document.querySelectorAll(".alert:not(.alert-dismissible)");
  alerts.forEach((alert) => {
    if (
      alert.classList.contains("alert-success") ||
      alert.classList.contains("alert-info")
    ) {
      alert.classList.add("alert-autohide");
    }
  });
  const autohideAlerts = document.querySelectorAll(".alert-autohide");
  autohideAlerts.forEach((alert) => {
    setTimeout(() => {
      alert.remove();
    }, 5000);
  });
});
