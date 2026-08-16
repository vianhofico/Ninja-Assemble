using System;
using System.Collections.Generic;

namespace NinjaAssemble.UI
{
    public sealed class NavigationStack
    {
        private readonly Stack<ScreenId> stack = new();
        public ScreenId? Current => stack.Count == 0 ? null : stack.Peek();
        public event Action<ScreenId> Changed;

        public void Reset(ScreenId screen)
        {
            stack.Clear();
            stack.Push(screen);
            Changed?.Invoke(screen);
        }

        public void Push(ScreenId screen)
        {
            stack.Push(screen);
            Changed?.Invoke(screen);
        }

        public bool Back()
        {
            if (stack.Count <= 1) return false;
            stack.Pop();
            Changed?.Invoke(stack.Peek());
            return true;
        }
    }
}
