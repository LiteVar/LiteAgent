import { useState } from "react";
import InputField from "../form/input/InputField";
import Button from "../ui/button/Button";

interface SearchFormProps {
  onSearch: (searchTerm: string) => void;
  loading: boolean;
  initialValue?: string;
}

export default function SearchForm({ onSearch, loading, initialValue = "" }: SearchFormProps) {
  const [searchTerm, setSearchTerm] = useState(initialValue);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch(searchTerm);
  };

  return (
    <form onSubmit={handleSubmit} className="flex w-full">
      <InputField
        type="text"
        placeholder="搜索机器人ID或Agent..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        className="!w-[300px]"
      />
      <Button
        size="sm"
        variant="outline"
        disabled={loading}
        className="ml-4"
      >
        搜索
      </Button>
    </form>
  );
}