import { useNavigate } from 'react-router-dom';
import {ROUTES} from "@/constants/routes";

type HandleBackClickType = () => void;

export const useHandleBackNavigation = (): HandleBackClickType => {
  const navigate = useNavigate();

  const handleBackNavigation = () => {
    const previousPath = document.referrer;
    if (window.history.length > 1 && previousPath) {
      const previousUrl = new URL(previousPath);
      const previousPathname = previousUrl.pathname;

      if (previousPathname === ROUTES.LOGIN) {
        navigate(ROUTES.DASHBOARD);
      } else {
        navigate(-1);
      }
    } else {
      navigate(ROUTES.DASHBOARD);
    }
  };

  return handleBackNavigation;
};
