import SignInForm from "../components/auth/SignInForm";
import PageMeta from "../components/common/PageMeta";

export default function SingIn() {
  return (
    <>
      <PageMeta
        title="登录 | LiteAgent DingTalk Admin"
        description="登录"
      />
      <SignInForm />
    </>
  );
}