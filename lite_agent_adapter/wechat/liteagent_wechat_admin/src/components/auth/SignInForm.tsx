import { useState, useCallback } from "react";
import { Navigate } from "react-router";
import { EyeCloseIcon, EyeIcon } from "../../icons";
import Label from "../form/Label";
import Input from "../form/input/InputField";
import Button from "../ui/button/Button";
import { useAuth } from "../../context/AuthContext";

interface FormData {
  username: string;
  password: string;
}

interface FormErrors {
  username?: string;
  password?: string;
  general?: string;
}

export default function SignInForm() {
  const { login, authState, isLoading } = useAuth();
  
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState<FormData>({
    username: "",
    password: "",
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [rememberMe, setRememberMe] = useState(false);

  const validateForm = useCallback((): boolean => {
    const newErrors: FormErrors = {};

    if (!formData.username.trim()) {
      newErrors.username = "用户名不能为空";
    } else if (formData.username.trim().length < 2) {
      newErrors.username = "用户名至少需要2个字符";
    }

    if (!formData.password) {
      newErrors.password = "密码不能为空";
    } else if (formData.password.length < 4) {
      newErrors.password = "密码至少需要4个字符";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData]);

  const handleInputChange = useCallback((field: keyof FormData) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = e.target.value;
    setFormData(prev => ({ ...prev, [field]: value }));
    
    // Clear field-specific error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    }
    
    // Clear general error when user makes changes
    if (errors.general) {
      setErrors(prev => ({ ...prev, general: undefined }));
    }
  }, [errors]);

  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    try {
      const result = await login(formData.username.trim(), formData.password);
      
      if (!result.success) {
        setErrors({ general: result.error || "登录失败" });
      }
      // Success case is handled by AuthContext (redirect via Navigate component)
    } catch (error) {
      console.error("Unexpected login error:", error);
      setErrors({ general: "发生意外错误，请重试" });
    }
  }, [formData, login, validateForm]);

  // Redirect if already authenticated
  if (authState === "authenticated") {
    return <Navigate to="/" replace />;
  }


  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 px-4 py-12">
      <div className="w-full max-w-md">
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700 p-8">
          <div className="mb-5 sm:mb-8">
            <h1 className="mb-3 font-bold text-gray-900 text-2xl dark:text-white text-center">
              登录
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-400 text-center">
              请登录您的账户以继续使用
            </p>
          </div>
          
          <div>
            <form onSubmit={handleSubmit} noValidate>
              <div className="space-y-6">
                {/* General Error Message */}
                {errors.general && (
                  <div className="p-4 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg dark:bg-red-900/20 dark:border-red-800 dark:text-red-400">
                    {errors.general}
                  </div>
                )}

                {/* Username Field */}
                <div>
                  <Label>
                    用户名 <span className="text-error-500">*</span>
                  </Label>
                  <Input
                    type="text"
                    placeholder="请输入用户名"
                    value={formData.username}
                    onChange={handleInputChange("username")}
                    className={errors.username ? "border-red-500 focus:border-red-500" : ""}
                    disabled={isLoading}
                    aria-describedby={errors.username ? "username-error" : undefined}
                  />
                  {errors.username && (
                    <p id="username-error" className="mt-1 text-sm text-red-600 dark:text-red-400">
                      {errors.username}
                    </p>
                  )}
                </div>

                {/* Password Field */}
                <div>
                  <Label>
                    密码 <span className="text-error-500">*</span>
                  </Label>
                  <div className="relative">
                    <Input
                      type={showPassword ? "text" : "password"}
                      placeholder="请输入密码"
                      value={formData.password}
                      onChange={handleInputChange("password")}
                      className={errors.password ? "border-red-500 focus:border-red-500" : ""}
                      disabled={isLoading}
                      aria-describedby={errors.password ? "password-error" : undefined}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2 hover:opacity-70 transition-opacity disabled:cursor-not-allowed disabled:opacity-50"
                      disabled={isLoading}
                      aria-label={showPassword ? "隐藏密码" : "显示密码"}
                    >
                      {showPassword ? (
                        <EyeIcon className="fill-gray-500 dark:fill-gray-400 size-5" />
                      ) : (
                        <EyeCloseIcon className="fill-gray-500 dark:fill-gray-400 size-5" />
                      )}
                    </button>
                  </div>
                  {errors.password && (
                    <p id="password-error" className="mt-1 text-sm text-red-600 dark:text-red-400">
                      {errors.password}
                    </p>
                  )}
                </div>

                {/* Remember Me */}
                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="remember-me"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-800 dark:focus:ring-blue-600"
                    disabled={isLoading}
                  />
                  <label
                    htmlFor="remember-me"
                    className="ml-2 text-sm text-gray-600 dark:text-gray-400 cursor-pointer"
                  >
                    记住我
                  </label>
                </div>
                
                {/* Submit Button */}
                <div>
                  <Button 
                    className="w-full relative h-12 bg-blue-600 hover:bg-blue-700 focus:ring-4 focus:ring-blue-300 dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800 transition-colors duration-200"
                    size="sm"
                    disabled={isLoading || !formData.username.trim() || !formData.password}
                    onClick={() => handleSubmit({ preventDefault: () => {} } as React.FormEvent)}
                  >
                    {isLoading ? (
                      <>
                        <span className="opacity-0">登录</span>
                        <div className="absolute inset-0 flex items-center justify-center">
                          <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        </div>
                      </>
                    ) : (
                      "登录"
                    )}
                  </Button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
