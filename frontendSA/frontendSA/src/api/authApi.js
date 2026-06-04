import axiosInstance from "./axiosInstance";

export const loginApi = (data) => {
  return axiosInstance.post("/auth/login", data);
};

export const getCurrentUserApi = () => {
  return axiosInstance.get("/auth/me");
};

export const changePasswordApi = (userId, data) => {
  return axiosInstance.put(`/auth/user/${userId}/password`, data);
};